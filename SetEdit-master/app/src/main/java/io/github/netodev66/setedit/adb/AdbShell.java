package io.github.netodev66.setedit.adb;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.muntashirakon.adb.AdbStream;
import io.github.muntashirakon.adb.LocalServices;

/**
 * Runs one shell command at a time over the app's ADB connection.
 *
 * <p>Each command opens its own {@code shell:} stream and writes the command followed by an exit
 * marker, so the exit code can be read without keeping a long-lived stream between calls. The
 * echoed command and the shell prompt are filtered out; {@code adbd} merges stderr into the shell
 * output, so the output is reported as-is.
 *
 * <p>Every call is bounded by {@link #TIMEOUT_MILLIS}. The transport is a raw socket whose
 * {@code isConnected()} keeps reporting true after the peer disappears, so a command issued once
 * WiFi is gone would otherwise block until the OS gives up on the connection. On timeout the
 * connection is closed, which unblocks the reader and lets the caller fall back to the provider.
 */
public final class AdbShell {
    private static final String EXIT_MARKER = "__SETEDIT_EXIT__";
    private static final Pattern EXIT_PATTERN = Pattern.compile(EXIT_MARKER + "(\\d+)");
    private static final long TIMEOUT_MILLIS = 10_000L;

    private static final ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "setedit-adb-io");
        thread.setDaemon(true);
        return thread;
    });

    private AdbShell() {
    }

    public static final class Result {
        @NonNull
        public final List<String> output;
        public final int exitCode;

        Result(@NonNull List<String> output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }
    }

    @WorkerThread
    @NonNull
    public static Result exec(@NonNull Context context, @NonNull String command) throws Exception {
        Future<Result> future = IO_EXECUTOR.submit(() -> execBlocking(context, command));
        try {
            return future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            // Closing the connection unblocks the reader that is still parked on the dead socket.
            try {
                AdbConnectionManager.getInstance(context).disconnect();
            } catch (Throwable ignored) {
            }
            throw new TimeoutException("adb command timed out after " + TIMEOUT_MILLIS + " ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IOException(cause);
        }
    }

    @NonNull
    private static Result execBlocking(@NonNull Context context, @NonNull String command) throws Exception {
        AdbConnectionManager manager = AdbConnectionManager.getInstance(context);
        List<String> output = new ArrayList<>();
        int exitCode = 1;
        try (AdbStream stream = manager.openStream(LocalServices.SHELL)) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream.openInputStream(), StandardCharsets.UTF_8));
            OutputStream out = stream.openOutputStream();
            out.write(("(" + command + ") ; echo " + EXIT_MARKER + "$?\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = EXIT_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    exitCode = Integer.parseInt(matcher.group(1));
                    break;
                }
                // An interactive shell echoes the command and its prompt; both contain the command.
                if (line.contains(command)) continue;
                output.add(line);
            }
        }
        return new Result(output, exitCode);
    }
}
