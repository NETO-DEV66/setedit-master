package io.github.netodev66.setedit.adb;

import android.content.Context;

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
import io.github.netodev66.setedit.data.AppPrefs;
import io.github.netodev66.setedit.utils.ShellExecutor;

/**
 * Owns the app's connection to the device's own ADB daemon over wireless debugging.
 *
 * <p>Discovery uses mDNS, so the connection follows the port that wireless debugging picks each
 * time it is turned on. Pairing happens once; afterwards the daemon already knows this app's key
 * and {@link #connect} succeeds silently.
 *
 * <p>A successful connect records the host and port it used, and every later connect tries that
 * endpoint first. mDNS discovery is the slow and unreliable half of reconnecting — it gives up
 * after a few seconds when the phone is busy — so skipping it whenever a known-good endpoint is
 * remembered is what keeps a paired device from ever asking the user to pair again.
 */
public final class AdbConnector {
    public enum State {DISCONNECTED, CONNECTING, CONNECTED, NEEDS_PAIRING}

    /** How long mDNS discovery may take before giving up. */
    public static final long DISCOVERY_TIMEOUT_MILLIS = 3000L;

    /**
     * How long a single connect attempt may take. The remembered-endpoint path needs only a TCP
     * handshake, so it stays quick, while discovery gets the full budget.
     */
    private static final long CONNECT_TIMEOUT_MILLIS = 5000L;

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
     * <p>First tries the host and port a previous connect recorded, which needs no discovery and is
     * therefore the fast, reliable path. Only when that fails does it fall back to mDNS discovery of
     * the TLS connect service and then the plain ADB service.
     * {@link State#NEEDS_PAIRING} means the daemon rejected this app's key, so the user has to pair.
     */
    @WorkerThread
    @NonNull
    public static State connect(@NonNull Context context) {
        STATE.set(State.CONNECTING);
        try {
            AbsAdbConnectionManager manager = AdbConnectionManager.getInstance(context);
            if (manager.isConnected()) {
                STATE.set(State.CONNECTED);
                AppPrefs.setPrivilegeGrantedOnce(context, true);
                return State.CONNECTED;
            }
            manager.setThrowOnUnauthorised(true);
            manager.setTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            String host = AppPrefs.getAdbHost(context);
            int port = AppPrefs.getAdbPort(context);
            if (host != null && port > 0) {
                boolean ok;
                try {
                    ok = attempt(manager, () -> manager.connect(host, port));
                } catch (AdbPairingRequiredException | AdbAuthenticationFailedException e) {
                    // The remembered port may now belong to something else that refuses this app's
                    // key. That is a stale endpoint, not a real pairing problem, so fall through to
                    // discovery instead of sending the user back to the pairing dialog.
                    ok = false;
                }
                if (ok) return connected(context, host, port);
                AppPrefs.clearAdbEndpoint(context);
            }

            Discovered discovered = discoverConnectService(context);
            if (discovered != null
                    && attempt(manager, () -> manager.connect(discovered.host, discovered.port))) {
                return connected(context, discovered.host, discovered.port);
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
     * Finds the ADB daemon's connect port over mDNS.
     *
     * <p>The daemon advertises a fresh port each time wireless debugging is turned on, so discovery
     * is what a first connection needs. The TLS connect service is preferred; the plain ADB service
     * is tried when it is not advertised.
     */
    @Nullable
    private static Discovered discoverConnectService(@NonNull Context context) {
        Discovered tls = discover(context, AdbMdns.SERVICE_TYPE_TLS_CONNECT, DISCOVERY_TIMEOUT_MILLIS);
        return tls != null
                ? tls : discover(context, AdbMdns.SERVICE_TYPE_ADB, DISCOVERY_TIMEOUT_MILLIS);
    }

    @Nullable
    private static Discovered discover(@NonNull Context context, @NonNull String serviceType,
                                       long timeoutMillis) {
        CountDownLatch found = new CountDownLatch(1);
        AtomicReference<String> host = new AtomicReference<>();
        AtomicInteger port = new AtomicInteger(-1);
        AdbMdns mdns = new AdbMdns(context, serviceType, (address, p) -> {
            if (address != null && p > 0) {
                host.set(address.getHostAddress());
                port.set(p);
                found.countDown();
            }
        });
        try {
            mdns.start();
            found.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mdns.stop();
        }
        return host.get() == null || port.get() <= 0 ? null : new Discovered(host.get(), port.get());
    }

    /** Marks a successful connect: remembers the endpoint, records the pairing and grants settings. */
    @NonNull
    private static State connected(@NonNull Context context, @Nullable String host, int port) {
        if (host != null && port > 0) AppPrefs.setAdbEndpoint(context, host, port);
        AppPrefs.setAdbPaired(context, true);
        AppPrefs.setPrivilegeGrantedOnce(context, true);
        STATE.set(State.CONNECTED);
        lastError = null;
        grantSecureSettings(context);
        return State.CONNECTED;
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
        return discover(context, AdbMdns.SERVICE_TYPE_TLS_PAIRING, timeoutMillis);
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
     * Grants the app the settings and usage permissions through the shell that is now connected.
     *
     * <p>The heavy lifting lives in {@link ShellExecutor#grantPrivilegedPermissions}, so the same
     * set is granted whether the shell came from ADB, Shizuku or root. Skipped once the permissions
     * are already held, since the package manager keeps them across a WiFi drop and a reboot.
     */
    private static void grantSecureSettings(@NonNull Context context) {
        if (ShellExecutor.hasPrivilegedPermissions(context)) return;
        ShellExecutor.grantPrivilegedPermissions(context);
    }
}
