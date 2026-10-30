package io.github.netodev66.setedit.adb;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbAuthenticationFailedException;
import io.github.muntashirakon.adb.AdbPairingRequiredException;
import io.github.muntashirakon.adb.android.AdbMdns;

/**
 * Owns the app's connection to the device's own ADB daemon over wireless debugging.
 *
 * <p>Discovery uses mDNS, so the connection follows the port that wireless debugging picks each
 * time it is turned on. Pairing happens once; afterwards the daemon already knows this app's key
 * and {@link #connect} succeeds silently.
 */
public final class AdbConnector {
    public enum State {DISCONNECTED, CONNECTING, CONNECTED, NEEDS_PAIRING}

    /** How long mDNS discovery may take before giving up. */
    public static final long DISCOVERY_TIMEOUT_MILLIS = 3000L;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-adb");
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicReference<State> STATE = new AtomicReference<>(State.DISCONNECTED);

    @Nullable
    private static volatile String lastError;

    private AdbConnector() {
    }

    @NonNull
    public static State getState() {
        return STATE.get();
    }

    public static boolean isConnected() {
        return STATE.get() == State.CONNECTED;
    }

    /**
     * Records that the transport is dead, so {@link #isConnected()} stops reporting {@code true}
     * for a socket the peer has already dropped, and closes the underlying connection so the next
     * {@link #connect} builds a fresh one.
     *
     * <p>Called by the shell layer when a command fails. Without this the cached state would keep
     * {@code ShellExecutor.isAvailable()} true after WiFi is gone and every later command would
     * block on the dead socket instead of falling back to the provider.
     */
    public static void onTransportLost(@NonNull Context context) {
        STATE.set(State.DISCONNECTED);
        try {
            AdbConnectionManager.getInstance(context).disconnect();
        } catch (Throwable ignored) {
        }
    }

    /** Runs {@code work} on the shared background thread. */
    public static void runAsync(@NonNull Runnable work) {
        EXECUTOR.execute(work);
    }

    @Nullable
    public static String getLastError() {
        return lastError;
    }

    /**
     * Connects to the already-paired ADB daemon.
     *
     * <p>Tries, in order, the TLS connect service that wireless debugging advertises, the plain
     * ADB service, and finally the classic TCP port. {@link State#NEEDS_PAIRING} means the daemon
     * rejected this app's key, so the user has to pair.
     */
    @WorkerThread
    @NonNull
    public static State connect(@NonNull Context context) {
        STATE.set(State.CONNECTING);
        try {
            AbsAdbConnectionManager manager = AdbConnectionManager.getInstance(context);
            if (manager.isConnected()) {
                STATE.set(State.CONNECTED);
                return State.CONNECTED;
            }
            manager.setThrowOnUnauthorised(true);
            manager.setTimeout(DISCOVERY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            if (attempt(manager, () -> manager.connect(5555))
                    || attempt(manager, () -> manager.connectTls(context, DISCOVERY_TIMEOUT_MILLIS))
                    || attempt(manager, () -> manager.connectTcp(context, DISCOVERY_TIMEOUT_MILLIS))) {
                STATE.set(State.CONNECTED);
                lastError = null;
                grantSecureSettings(context);
                return State.CONNECTED;
            }
            // Discovery never found a daemon: wireless debugging is most likely off.
            STATE.set(State.DISCONNECTED);
        } catch (AdbPairingRequiredException | AdbAuthenticationFailedException e) {
            STATE.set(State.NEEDS_PAIRING);
            lastError = e.getMessage();
        } catch (Throwable th) {
            STATE.set(State.DISCONNECTED);
            lastError = th.getMessage();
        }
        return STATE.get();
    }

    /**
     * Runs one connection attempt.
     *
     * @return {@code true} when connected; {@code false} when the daemon could not be found, so the
     * caller may try another transport. Authentication failures propagate so the caller can ask
     * the user to pair.
     */
    private static boolean attempt(@NonNull AbsAdbConnectionManager manager, @NonNull ConnectCall call)
            throws Exception {
        try {
            return call.run();
        } catch (AdbPairingRequiredException | AdbAuthenticationFailedException e) {
            throw e;
        } catch (Throwable th) {
            lastError = th.getMessage();
            return false;
        }
    }

    private interface ConnectCall {
        boolean run() throws Exception;
    }

    /** A host/port pair discovered over mDNS. */
    public static final class Discovered {
        @NonNull
        public final String host;
        public final int port;

        Discovered(@NonNull String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    /** Discovers the host and port that the wireless-debugging pairing dialog is listening on. */
    @WorkerThread
    @Nullable
    public static Discovered discoverPairingService(@NonNull Context context, long timeoutMillis) {
        AtomicInteger port = new AtomicInteger(-1);
        AtomicReference<String> host = new AtomicReference<>(null);
        CountDownLatch resolved = new CountDownLatch(1);
        AdbMdns mdns = new AdbMdns(context, AdbMdns.SERVICE_TYPE_TLS_PAIRING, (hostAddress, foundPort) -> {
            if (hostAddress != null) {
                host.set(hostAddress.getHostAddress());
                port.set(foundPort);
            }
            resolved.countDown();
        });
        mdns.start();
        try {
            if (!resolved.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            mdns.stop();
        }
        String foundHost = host.get();
        int foundPort = port.get();
        if (foundHost == null || foundPort == -1) return null;
        return new Discovered(foundHost, foundPort);
    }

    /** Pairs with the daemon using a code shown in the wireless-debugging dialog. */
    @WorkerThread
    public static boolean pair(@NonNull Context context, @NonNull String host, int port,
                               @NonNull String pairingCode) {
        try {
            AbsAdbConnectionManager manager = AdbConnectionManager.getInstance(context);
            return manager.pair(host, port, pairingCode);
        } catch (Throwable th) {
            lastError = th.getMessage();
            return false;
        }
    }

    /**
     * Finds the pairing service, pairs with the given code, then connects.
     *
     * @return {@link State#CONNECTED} on success, otherwise the state that describes what failed.
     */
    @WorkerThread
    @NonNull
    public static State pairAndConnect(@NonNull Context context, @NonNull String pairingCode) {
        STATE.set(State.CONNECTING);
        try {
            // Make sure the identity exists before pairing, so it survives even if pairing fails.
            AdbConnectionManager.getInstance(context);
        } catch (Throwable th) {
            lastError = th.getMessage();
            STATE.set(State.DISCONNECTED);
            return State.DISCONNECTED;
        }
        Discovered service = discoverPairingService(context, DISCOVERY_TIMEOUT_MILLIS);
        if (service == null) {
            STATE.set(State.DISCONNECTED);
            lastError = "pairing service not found";
            return State.DISCONNECTED;
        }
        if (!pair(context, service.host, service.port, pairingCode)) {
            STATE.set(State.DISCONNECTED);
            return State.DISCONNECTED;
        }
        return connect(context);
    }

    public static void disconnect(@NonNull Context context) {
        runAsync(() -> {
            try {
                AdbConnectionManager.getInstance(context).disconnect();
            } catch (Throwable ignored) {
            }
            STATE.set(State.DISCONNECTED);
        });
    }

    /** Drops any stale connection and connects again. Returns whether it is connected afterwards. */
    @WorkerThread
    public static boolean reconnect(@NonNull Context context) {
        try {
            AdbConnectionManager.getInstance(context).disconnect();
        } catch (Throwable ignored) {
        }
        STATE.set(State.DISCONNECTED);
        return connect(context) == State.CONNECTED;
    }

    /**
     * Makes the app's own settings writes work without the ADB connection.
     *
     * <p>The {@code SettingsProvider} lets a caller write {@code system}/{@code secure}/{@code
     * global} rows only with {@code WRITE_SECURE_SETTINGS} (or {@code WRITE_SETTINGS} for the system
     * table), yet it never hands that permission to an app on its own. The ADB shell identity can
     * grant it, so it is granted here, once, while the connection is up. The grant is stored by the
     * package manager and survives both a WiFi drop and a reboot, which is what lets SetEdit keep
     * editing settings with no network at all.
     */
    private static void grantSecureSettings(@NonNull Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String pkg = context.getPackageName();
        try {
            AdbShell.exec(context, "pm grant " + pkg + " " + Manifest.permission.WRITE_SECURE_SETTINGS);
            AdbShell.exec(context, "pm grant " + pkg + " " + Manifest.permission.WRITE_SETTINGS);
            AdbShell.exec(context, "appops set " + pkg + " WRITE_SETTINGS allow");
        } catch (Throwable ignored) {
        }
    }
}
