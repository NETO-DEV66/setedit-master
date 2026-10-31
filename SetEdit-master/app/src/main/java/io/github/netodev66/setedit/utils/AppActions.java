package io.github.netodev66.setedit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Runs the app-manager actions through the privileged shell.
 *
 * <p>All calls are blocking network/process I/O and must run off the main thread. Each method
 * returns an {@link AppActionResult} instead of throwing, so the UI can show a plain success or
 * failure toast either way.
 */
public final class AppActions {

    private AppActions() {
    }

    @NonNull
    private static AppActionResult run(@NonNull String command) {
        return AppActionResult.of(ShellExecutor.execWithRetry(command));
    }

    @NonNull
    public static AppActionResult forceStop(@NonNull String pkg) {
        return run(AppCommands.forceStop(pkg));
    }

    @NonNull
    public static AppActionResult clearCache(@NonNull String pkg) {
        return run(AppCommands.clearCache(pkg));
    }

    @NonNull
    public static AppActionResult clearData(@NonNull String pkg) {
        return run(AppCommands.clearData(pkg));
    }

    @NonNull
    public static AppActionResult setEnabled(@NonNull String pkg, boolean enabled) {
        return run(enabled ? AppCommands.enable(pkg) : AppCommands.disable(pkg));
    }

    @NonNull
    public static AppActionResult uninstall(@NonNull String pkg, boolean system) {
        return run(system ? AppCommands.uninstallForUser(pkg) : AppCommands.uninstall(pkg));
    }

    @NonNull
    public static AppActionResult setSuspended(@NonNull String pkg, boolean suspended) {
        return run(suspended ? AppCommands.suspend(pkg) : AppCommands.unsuspend(pkg));
    }

    @NonNull
    public static AppActionResult setPermission(@NonNull String pkg, @NonNull String permission,
                                                boolean granted) {
        return run(granted ? AppCommands.grant(pkg, permission)
                : AppCommands.revoke(pkg, permission));
    }

    @NonNull
    public static AppActionResult setAppOp(@NonNull String pkg, @NonNull String op,
                                           @NonNull String mode) {
        return run(AppCommands.appOpSet(pkg, op, mode));
    }

    @NonNull
    public static AppActionResult setBatteryWhitelisted(@NonNull String pkg, boolean whitelisted) {
        return run(AppCommands.batteryWhitelist(pkg, whitelisted));
    }

    @NonNull
    public static AppActionResult setStandbyBucket(@NonNull String pkg, @NonNull String bucket) {
        return run(AppCommands.standbyBucketSet(pkg, bucket));
    }

    /** Reads the current AppOps mode for one op, or {@code null} when it cannot be parsed. */
    @Nullable
    public static String appOpMode(@NonNull String pkg, @NonNull String op) {
        AppActionResult result = run(AppCommands.appOpGet(pkg, op));
        if (!result.successful) return null;
        // Output looks like: "WIFI_SCAN: allow" (older builds) or "WIFI_SCAN: mode=allow".
        for (String line : result.output) {
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String value = line.substring(colon + 1).trim();
            int equals = value.indexOf('=');
            if (equals >= 0) value = value.substring(equals + 1).trim();
            value = value.toLowerCase(Locale.ROOT);
            if (AppCommands.OP_MODES.contains(value)) return value;
        }
        return null;
    }

    /** True when the package is in the battery-optimisation exemption list. */
    public static boolean isBatteryWhitelisted(@NonNull String pkg) {
        AppActionResult result = run(AppCommands.batteryWhitelistQuery());
        if (!result.successful) return false;
        for (String line : result.output) {
            if (line.contains(pkg)) return true;
        }
        return false;
    }

    /** The current standby bucket name, or {@code null} when it cannot be read. */
    @Nullable
    public static String standbyBucket(@NonNull String pkg) {
        AppActionResult result = run(AppCommands.standbyBucketGet(pkg));
        if (!result.successful || result.output.isEmpty()) return null;
        String first = result.output.get(0).trim();
        return AppCommands.STANDBY_BUCKETS.contains(first) ? first : null;
    }

    /** A tiny helper so callers can tell whether privileged access is even possible right now. */
    public static boolean isPrivileged() {
        return ShellExecutor.isAvailable();
    }
}
