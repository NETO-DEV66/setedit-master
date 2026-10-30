package io.github.netodev66.setedit.update;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.BuildConfig;

/**
 * Coordinates the update check and the APK download.
 *
 * <p>Both are blocking, so they run on a single background executor and report back on the main
 * thread. The result of the last check is cached in memory so opening the update screen does not
 * hit the network again.
 */
public final class UpdateManager {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-update");
        thread.setDaemon(true);
        return thread;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Nullable
    private static volatile UpdateInfo cached;

    private UpdateManager() {
    }

    @Nullable
    public static UpdateInfo cached() {
        return cached;
    }

    public static void clearCache() {
        cached = null;
    }

    /** Runs a network check and posts the result on the main thread. */
    public static void check(@NonNull Context context, @NonNull Callback<UpdateInfo> callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.submit(() -> {
            UpdateInfo info = UpdateChecker.check(appContext);
            if (info.status != UpdateInfo.Status.FAILED) cached = info;
            MAIN.post(() -> callback.onResult(info));
        });
    }

    /** Downloads {@code info}'s APK into the cache dir and posts the file (or the failure). */
    public static void download(@NonNull Context context, @NonNull UpdateInfo info,
                                @NonNull DownloadCallback callback) {
        if (info.apkUrl == null) {
            MAIN.post(() -> callback.onError(new IllegalStateException("no apk asset")));
            return;
        }
        Context appContext = context.getApplicationContext();
        File target = new File(appContext.getCacheDir(), "SetEdit-" + info.version + ".apk");
        EXECUTOR.submit(() -> {
            try {
                UpdateChecker.download(info.apkUrl, target.getAbsolutePath(),
                        new UpdateChecker.ProgressListener() {
                            @Override
                            public void onStart(int totalBytes) {
                                MAIN.post(() -> callback.onStart(totalBytes));
                            }

                            @Override
                            public void onProgress(long bytesRead, int totalBytes) {
                                MAIN.post(() -> callback.onProgress(bytesRead, totalBytes));
                            }
                        });
                MAIN.post(() -> callback.onDownloaded(target));
            } catch (Exception e) {
                //noinspection ResultOfMethodCallIgnored
                target.delete();
                MAIN.post(() -> callback.onError(e));
            }
        });
    }

    /** Deletes a previously downloaded APK; safe to call with a stale path. */
    public static void discard(@Nullable File apk) {
        if (apk != null && apk.exists()) {
            //noinspection ResultOfMethodCallIgnored
            apk.delete();
        }
    }

    /** The installed version, without a leading {@code v}. */
    @NonNull
    public static String currentVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public interface Callback<T> {
        void onResult(@NonNull T value);
    }

    public interface DownloadCallback {
        void onStart(int totalBytes);

        void onProgress(long bytesRead, int totalBytes);

        void onDownloaded(@NonNull File apk);

        void onError(@NonNull Exception error);
    }
}
