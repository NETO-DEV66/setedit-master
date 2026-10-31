package io.github.netodev66.setedit.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.netodev66.setedit.adb.AdbConnector;
import io.github.netodev66.setedit.data.AppPrefs;
import rikka.shizuku.Shizuku;

/**
 * The one place that decides whether the app can act as the shell, and gets it there if it cannot.
 *
 * <p>Privileged access can come from three places: root, a Shizuku service the user has approved,
 * or the app's own ADB connection over wireless debugging. {@link #ensure} walks them in that order
 * and only reports failure when none can be made to work.
 *
 * <p>Two things keep the user from being asked again and again once a route has worked:
 * <ul>
 *   <li>Shizuku is actually asked for permission here ({@link Shizuku#requestPermission}). The app
 *       used to only read {@link Shizuku#checkSelfPermission()}, so a device running Shizuku looked
 *       like it had no access and the user was sent to the wireless-debugging screen for nothing.</li>
 *   <li>A device that has paired over ADB is remembered, so a later failure is retried silently
 *       instead of sending the user back to the pairing dialog.</li>
 * </ul>
 */
public final class PrivilegedAccess {

    /** How long to wait for the user to answer the Shizuku permission dialog. */
    private static final long SHIZUKU_PERMISSION_TIMEOUT_MILLIS = 20_000L;

    /** The Shizuku request code; only one request is ever in flight. */
    private static final int SHIZUKU_REQUEST_CODE = 4701;

    /**
     * A private single thread, so {@link #ensure} never queues behind the ADB executor it calls into
     * (which would deadlock) and two screens asking at once do not race the same connect.
     */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-privilege");
        thread.setDaemon(true);
        return thread;
    });

    private static final Object LOCK = new Object();
    private static boolean ensureInFlight;
    @NonNull
    private static final List<Callback> PENDING = new ArrayList<>();

    private PrivilegedAccess() {
    }

    /** The route that would be used right now, or {@link ShellExecutor.Mode#NONE}. */
    @NonNull
    public static ShellExecutor.Mode getMode() {
        return ShellExecutor.getMode();
    }

    /** True when a privileged shell is usable at this instant. */
    public static boolean isReady() {
        return ShellExecutor.isAvailable();
    }

    /**
     * True when this device has granted privileged access at least once, so the app should retry
     * silently rather than show the connect screen. Root and Shizuku are read live; the ADB route
     * is remembered because the connection itself can drop while the pairing stays valid.
     */
    public static boolean hasEverHadAccess(@NonNull Context context) {
        if (Boolean.TRUE.equals(com.topjohnwu.superuser.Shell.isAppGrantedRoot())) return true;
        if (ShellExecutor.isShizukuReady()) return true;
        return AppPrefs.isAdbPaired(context) || AppPrefs.isPrivilegeGrantedOnce(context);
    }

    /** True when the user must open the connect screen, because no route has ever worked. */
    public static boolean needsUserSetup(@NonNull Context context) {
        return !isReady() && !hasEverHadAccess(context);
    }

    /** Callback for {@link #ensure}; always invoked on the main thread. */
    public interface Callback {
        /**
         * @param ready  whether a privileged shell is usable now
         * @param reason a short, already-localised explanation when {@code ready} is false
         */
        void onResult(boolean ready, @NonNull String reason);
    }

    /**
     * Makes sure a privileged shell is usable, silently where possible.
     *
     * <p>Runs entirely on a background thread and reports back on the main thread. Tries, in order:
     * an already-available shell, a Shizuku permission request, then an ADB reconnect when this
     * device has paired before.
     */
    public static void ensure(@NonNull Context context, @NonNull Callback callback) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (ensureInFlight) {
                // A run is already going; ride along instead of racing it.
                PENDING.add(callback);
                return;
            }
            ensureInFlight = true;
            PENDING.add(callback);
        }
        EXECUTOR.execute(() -> {
            String reason = "";
            boolean ready = false;
            if (ShellExecutor.isAvailable()) {
                ready = true;
            } else if (tryShizuku(appContext)) {
                ready = true;
            } else if (AppPrefs.isAdbPaired(appContext)
                    && AdbConnector.reconnect(appContext)) {
                ready = true;
            } else {
                reason = describeFailure(appContext);
            }
            if (ready) {
                // One pass through whatever route worked leaves the app holding the settings and
                // usage permissions, so later actions never need the shell again.
                if (!ShellExecutor.hasPrivilegedPermissions(appContext)) {
                    ShellExecutor.grantPrivilegedPermissions(appContext);
                }
                AppPrefs.setPrivilegeGrantedOnce(appContext, true);
            }
            final boolean result = ready;
            final String message = reason;
            List<Callback> waiting;
            synchronized (LOCK) {
                ensureInFlight = false;
                waiting = new ArrayList<>(PENDING);
                PENDING.clear();
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                for (Callback pending : waiting) pending.onResult(result, message);
            });
        });
    }

    /**
     * Requests the Shizuku permission when a Shizuku service is running but this app has not been
     * approved yet, then waits for the answer.
     *
     * <p>The request has to be issued from the main thread, so it is posted there while the calling
     * background thread waits on a latch.
     *
     * @return {@code true} when Shizuku is ready to use afterwards
     */
    @WorkerThread
    private static boolean tryShizuku(@NonNull Context context) {
        if (ShellExecutor.isShizukuReady()) return true;
        if (!ShellExecutor.isShizukuBinderAvailable()) return false;
        // The user already turned this app down once; asking again on every screen would nag. The
        // connect screen offers the prompt explicitly for that case, and clears this flag.
        if (AppPrefs.isShizukuDenied(context)) return false;

        CountDownLatch answered = new CountDownLatch(1);
        AtomicInteger grantResult = new AtomicInteger(-1);
        Shizuku.OnRequestPermissionResultListener listener = (requestCode, result) -> {
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                grantResult.set(result);
                answered.countDown();
            }
        };
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Shizuku.addRequestPermissionResultListener(listener);
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
            } catch (Throwable th) {
                answered.countDown();
            }
        });
        try {
            answered.await(SHIZUKU_PERMISSION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            new Handler(Looper.getMainLooper()).post(() ->
                    Shizuku.removeRequestPermissionResultListener(listener));
        }
        if (ShellExecutor.isShizukuReady()) {
            AppPrefs.setShizukuDenied(context, false);
            return true;
        }
        // A refusal (or no answer at all) is remembered so the prompt is not repeated everywhere.
        if (grantResult.get() != 0) AppPrefs.setShizukuDenied(context, true);
        return false;
    }

    /** A short, localised reason for the failure, so the caller can show it without guessing. */
    @NonNull
    private static String describeFailure(@NonNull Context context) {
        if (ShellExecutor.isShizukuBinderAvailable()) {
            return context.getString(io.github.netodev66.setedit.R.string.app_access_denied);
        }
        if (AppPrefs.isAdbPaired(context)) {
            return context.getString(io.github.netodev66.setedit.R.string.app_access_offline);
        }
        return context.getString(io.github.netodev66.setedit.R.string.app_needs_privilege);
    }

    /** {@code true} when a Shizuku service is running on this device, whether or not it is granted. */
    public static boolean isShizukuAvailable() {
        return ShellExecutor.isShizukuBinderAvailable();
    }

    /** The last connect error, for diagnostics; may be {@code null}. */
    @Nullable
    public static String lastError() {
        return AdbConnector.getLastError();
    }
}
