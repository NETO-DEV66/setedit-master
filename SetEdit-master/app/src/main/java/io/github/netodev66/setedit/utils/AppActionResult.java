package io.github.netodev66.setedit.utils;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/** The outcome of an app-manager action: whether it worked and any output to show. */
public final class AppActionResult {

    public final boolean successful;
    /** Combined stdout, useful for the read-only queries (appops, standby bucket, whitelist). */
    @NonNull
    public final List<String> output;
    @NonNull
    public final String error;

    public AppActionResult(boolean successful, @NonNull List<String> output, @NonNull String error) {
        this.successful = successful;
        this.output = output;
        this.error = error;
    }

    @NonNull
    public static AppActionResult of(@NonNull ShellExecutor.Result result) {
        if (result.exitCode == ShellExecutor.EXIT_UNSUPPORTED) {
            return new AppActionResult(false, Collections.emptyList(), "no privileged access");
        }
        return new AppActionResult(result.isSuccess(), result.stdout, result.errorMessage());
    }
}
