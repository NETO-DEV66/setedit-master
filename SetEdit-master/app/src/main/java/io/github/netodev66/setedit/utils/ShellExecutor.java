package io.github.netodev66.setedit.utils;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.netodev66.setedit.adb.AdbConnector;
import io.github.netodev66.setedit.adb.AdbShell;
import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;

/**
 * Runs shell commands with the highest privilege the device allows.
 *
 * <p>Android 15 and later refuse to install an app that targets below API 24, and once the app
 * targets a modern API the {@code SettingsProvider} blocks it from reading or writing many
 * {@code system}/{@code secure}/{@code global} keys through {@code ContentResolver}. The provider
 * exempts the shell (uid 2000) and root identities, so every settings operation is issued through
 * this executor first and only falls back to {@code ContentResolver} when none is available.
 *
 * <p>Three ways to reach that shell identity are tried, in order: root (libsu), a running Shizuku
 * service, and the app's own ADB connection over wireless debugging.
 */
public final class ShellExecutor {
    /** Command execution is blocking, so callers on the main thread should not use it directly. */
    public enum Mode {ROOT, SHIZUKU, ADB, NONE}

    public static final int EXIT_UNSUPPORTED = Integer.MIN_VALUE;

    private static final long SHIZUKU_TIMEOUT_SECONDS = 15L;

    @Nullable
    private static volatile Context appContext;

    private ShellExecutor() {
    }

    /** Stores the application context used to reach the ADB connection. */
    public static void init(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    public static final class Result {
        @NonNull
        public final List<String> stdout;
        @NonNull
        public final List<String> stderr;
        public final int exitCode;

        private Result(@NonNull List<String> stdout, @NonNull List<String> stderr, int exitCode) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        @NonNull
        public String errorMessage() {
            return stderr.isEmpty() ? "" : android.text.TextUtils.join("\n", stderr);
        }
    }

    @NonNull
    public static Mode getMode() {
        if (Boolean.TRUE.equals(Shell.isAppGrantedRoot())) return Mode.ROOT;
        if (isShizukuReady()) return Mode.SHIZUKU;
        if (isAdbReady()) return Mode.ADB;
        return Mode.NONE;
    }

    public static boolean isAvailable() {
        return getMode() != Mode.NONE;
    }

    public static boolean isShizukuReady() {
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0;
        } catch (Throwable th) {
            return false;
        }
    }

    /** True once the app's own ADB connection is established. */
    public static boolean isAdbReady() {
        return AdbConnector.isConnected();
    }

    /** True once the Shizuku binder is present, whether or not this app has been granted access. */
    public static boolean isShizukuBinderAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable th) {
            return false;
        }
    }

    @NonNull
    public static Result exec(@NonNull String command) {
        switch (getMode()) {
            case ROOT:
                return execRoot(command);
            case SHIZUKU:
                return execShizuku(command);
            case ADB:
                return execAdb(command);
            default:
                return new Result(Collections.emptyList(), Collections.emptyList(), EXIT_UNSUPPORTED);
        }
    }

    @NonNull
    private static Result execAdb(@NonNull String command) {
        Context context = appContext;
        if (context == null) {
            return new Result(Collections.emptyList(),
                    Collections.singletonList("ADB context unavailable"), 1);
        }
        try {
            return toResult(AdbShell.exec(context, command));
        } catch (Throwable th) {
            // Any transport failure — a dead socket after WiFi is gone, or a stale one after adbd
            // restarted — means this connection is done. Mark it so isAvailable() stops reporting
            // true, and fall back to the provider. Reconnecting is deliberately NOT attempted here:
            // connect() can block for a long time on an unreachable port, which would hang this
            // write/read. The next app resume reconnects on its own thread instead.
            AdbConnector.onTransportLost(context);
            return new Result(Collections.emptyList(),
                    Collections.singletonList(String.valueOf(th.getMessage())), 1);
        }
    }

    @NonNull
    private static Result toResult(@NonNull AdbShell.Result result) {
        return new Result(result.output, Collections.emptyList(), result.exitCode);
    }

    @NonNull
    private static Result execRoot(@NonNull String command) {
        try {
            Shell.Result result = Shell.cmd(command).exec();
            return new Result(result.getOut(), result.getErr(), result.isSuccess() ? 0 : 1);
        } catch (Throwable th) {
            return new Result(Collections.emptyList(),
                    Collections.singletonList(String.valueOf(th.getMessage())), 1);
        }
    }

    @NonNull
    private static Result execShizuku(@NonNull String command) {
        IRemoteProcess process = null;
        try {
            IShizukuService service = IShizukuService.Stub.asInterface(Shizuku.getBinder());
            process = service.newProcess(new String[]{"sh", "-c", command}, null, null);
            // Drain stdout and stderr concurrently; a full pipe on either side would deadlock waitFor.
            StreamDrainer out = new StreamDrainer(process.getInputStream());
            StreamDrainer err = new StreamDrainer(process.getErrorStream());
            out.start();
            err.start();
            boolean finished = process.waitForTimeout(SHIZUKU_TIMEOUT_SECONDS, TimeUnit.SECONDS.name());
            if (!finished) process.destroy();
            out.join(2000L);
            err.join(2000L);
            int exit = finished ? process.exitValue() : 1;
            return new Result(out.lines, err.lines, exit);
        } catch (Throwable th) {
            return new Result(Collections.emptyList(),
                    Collections.singletonList(String.valueOf(th.getMessage())), 1);
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static final class StreamDrainer extends Thread {
        private final ParcelFileDescriptor descriptor;
        final List<String> lines = new ArrayList<>();

        StreamDrainer(@Nullable ParcelFileDescriptor descriptor) {
            this.descriptor = descriptor;
            setDaemon(true);
        }

        @Override
        public void run() {
            if (descriptor == null) return;
            try (InputStream raw = new FileInputStream(descriptor.getFileDescriptor());
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(raw, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException ignored) {
            } finally {
                try {
                    descriptor.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
