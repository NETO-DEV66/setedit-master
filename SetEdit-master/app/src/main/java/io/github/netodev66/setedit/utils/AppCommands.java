package io.github.netodev66.setedit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the shell commands the app manager runs.
 *
 * <p>Every command is assembled here as a plain string so the exact arguments can be unit tested
 * without an Android runtime. {@link AppActions} feeds the result to {@link ShellExecutor}.
 *
 * <p>Package names, permissions and op names are validated against a strict pattern before they are
 * interpolated. The values normally come from {@code PackageManager} and are already safe, but the
 * detail screen can also be opened from an {@code Intent} extra, so a malformed value must not be
 * able to smuggle a second shell command into the line.
 */
public final class AppCommands {

    /** Package segments are letters, digits and underscores, joined by dots ({@code android} is valid). */
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");
    private static final Pattern PERMISSION = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");
    private static final Pattern OP = Pattern.compile("[A-Z0-9_]+");

    /** The modes {@code appops set} accepts. */
    public static final List<String> OP_MODES = Arrays.asList("allow", "ignore", "deny", "default");
    /** The standby buckets {@code am set-standby-bucket} accepts. */
    public static final List<String> STANDBY_BUCKETS = Arrays.asList(
            "active", "working_set", "frequent", "rare", "restricted", "never");

    private AppCommands() {
    }

    public static boolean isValidPackage(@Nullable String value) {
        return value != null && PACKAGE.matcher(value).matches();
    }

    public static boolean isValidPermission(@Nullable String value) {
        return value != null && PERMISSION.matcher(value).matches();
    }

    public static boolean isValidOp(@Nullable String value) {
        return value != null && OP.matcher(value).matches();
    }

    public static boolean isValidOpMode(@Nullable String value) {
        return value != null && OP_MODES.contains(value);
    }

    public static boolean isValidBucket(@Nullable String value) {
        return value != null && STANDBY_BUCKETS.contains(value);
    }

    @NonNull
    public static String forceStop(@NonNull String pkg) {
        return "am force-stop " + pkg;
    }

    @NonNull
    public static String clearCache(@NonNull String pkg) {
        return "pm clear --cache-only " + pkg;
    }

    @NonNull
    public static String clearData(@NonNull String pkg) {
        return "pm clear " + pkg;
    }

    @NonNull
    public static String enable(@NonNull String pkg) {
        return "pm enable " + pkg;
    }

    @NonNull
    public static String disable(@NonNull String pkg) {
        return "pm disable-user --user 0 " + pkg;
    }

    @NonNull
    public static String uninstall(@NonNull String pkg) {
        return "pm uninstall " + pkg;
    }

    /** Removes a system app for the current user only; the APK stays in {@code /system}. */
    @NonNull
    public static String uninstallForUser(@NonNull String pkg) {
        return "pm uninstall --user 0 " + pkg;
    }

    @NonNull
    public static String suspend(@NonNull String pkg) {
        return "pm suspend --user 0 " + pkg;
    }

    @NonNull
    public static String unsuspend(@NonNull String pkg) {
        return "pm unsuspend --user 0 " + pkg;
    }

    @NonNull
    public static String grant(@NonNull String pkg, @NonNull String permission) {
        return "pm grant " + pkg + " " + permission;
    }

    @NonNull
    public static String revoke(@NonNull String pkg, @NonNull String permission) {
        return "pm revoke " + pkg + " " + permission;
    }

    @NonNull
    public static String appOpGet(@NonNull String pkg, @NonNull String op) {
        return "cmd appops get " + pkg + " " + op;
    }

    @NonNull
    public static String appOpSet(@NonNull String pkg, @NonNull String op, @NonNull String mode) {
        return "cmd appops set " + pkg + " " + op + " " + mode;
    }

    @NonNull
    public static String batteryWhitelist(@NonNull String pkg, boolean whitelist) {
        return "dumpsys deviceidle whitelist " + (whitelist ? "+" : "-") + pkg;
    }

    @NonNull
    public static String batteryWhitelistQuery() {
        return "dumpsys deviceidle whitelist";
    }

    @NonNull
    public static String standbyBucketGet(@NonNull String pkg) {
        return "am get-standby-bucket " + pkg;
    }

    @NonNull
    public static String standbyBucketSet(@NonNull String pkg, @NonNull String bucket) {
        return "am set-standby-bucket " + pkg + " " + bucket;
    }
}
