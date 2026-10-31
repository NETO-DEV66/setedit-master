package io.github.netodev66.setedit.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Single source of truth for the app's user preferences.
 *
 * <p>Everything that used to be scattered across ad-hoc {@code getSharedPreferences("prefs")}
 * calls lives here so a setting is read the same way from every screen.
 */
public final class AppPrefs {
    private static final String FILE = "setedit_prefs";

    private static final String KEY_THEME = "theme";
    private static final String KEY_DYNAMIC_COLOR = "dynamic_color";
    private static final String KEY_CONFIRM_RISKY = "confirm_risky";
    private static final String KEY_AUTO_RECONNECT = "auto_reconnect";
    private static final String KEY_SHOW_ADVANCED = "show_advanced";
    private static final String KEY_ONBOARDED = "onboarded";
    private static final String KEY_CONNECT_PROMPT_SHOWN = "connect_prompt_shown";
    private static final String KEY_WARNED = "has_warned";
    private static final String KEY_CHECK_UPDATES = "check_updates";
    private static final String KEY_LAST_UPDATE_CHECK = "last_update_check";
    private static final String KEY_REDUCE_MOTION = "reduce_motion";
    private static final String KEY_AUTO_BACKUP = "auto_backup";
    private static final String KEY_ADB_HOST = "adb_host";
    private static final String KEY_ADB_PORT = "adb_port";
    private static final String KEY_ADB_PAIRED = "adb_paired";
    private static final String KEY_PRIVILEGE_GRANTED_ONCE = "privilege_granted_once";
    private static final String KEY_SHIZUKU_DENIED = "shizuku_denied";

    private AppPrefs() {
    }

    @NonNull
    public static SharedPreferences get(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int getThemeMode(@NonNull Context context) {
        return get(context).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(@NonNull Context context, int mode) {
        get(context).edit().putInt(KEY_THEME, mode).apply();
    }

    public static boolean isDynamicColor(@NonNull Context context) {
        return get(context).getBoolean(KEY_DYNAMIC_COLOR, true);
    }

    public static void setDynamicColor(@NonNull Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply();
    }

    public static boolean isConfirmRisky(@NonNull Context context) {
        return get(context).getBoolean(KEY_CONFIRM_RISKY, true);
    }

    public static void setConfirmRisky(@NonNull Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_CONFIRM_RISKY, enabled).apply();
    }

    public static boolean isAutoReconnect(@NonNull Context context) {
        return get(context).getBoolean(KEY_AUTO_RECONNECT, true);
    }

    public static void setAutoReconnect(@NonNull Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply();
    }

    public static boolean isShowAdvanced(@NonNull Context context) {
        return get(context).getBoolean(KEY_SHOW_ADVANCED, true);
    }

    public static void setShowAdvanced(@NonNull Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_SHOW_ADVANCED, enabled).apply();
    }

    public static boolean isOnboarded(@NonNull Context context) {
        return get(context).getBoolean(KEY_ONBOARDED, false);
    }

    public static void setOnboarded(@NonNull Context context, boolean done) {
        get(context).edit().putBoolean(KEY_ONBOARDED, done).apply();
    }

    public static boolean isConnectPromptShown(@NonNull Context context) {
        return get(context).getBoolean(KEY_CONNECT_PROMPT_SHOWN, false);
    }

    public static void setConnectPromptShown(@NonNull Context context, boolean shown) {
        get(context).edit().putBoolean(KEY_CONNECT_PROMPT_SHOWN, shown).apply();
    }

    public static boolean isWarned(@NonNull Context context) {
        return get(context).getBoolean(KEY_WARNED, false);
    }

    public static void setWarned(@NonNull Context context, boolean warned) {
        get(context).edit().putBoolean(KEY_WARNED, warned).apply();
    }

    /** Whether the app should look for a newer release when it starts. */
    public static boolean isCheckUpdates(@NonNull Context context) {
        return get(context).getBoolean(KEY_CHECK_UPDATES, true);
    }

    public static void setCheckUpdates(@NonNull Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_CHECK_UPDATES, enabled).apply();
    }

    /** Epoch millis of the last successful or attempted update check, or {@code 0}. */
    public static long getLastUpdateCheck(@NonNull Context context) {
        return get(context).getLong(KEY_LAST_UPDATE_CHECK, 0L);
    }

    public static void setLastUpdateCheck(@NonNull Context context, long epochMillis) {
        get(context).edit().putLong(KEY_LAST_UPDATE_CHECK, epochMillis).apply();
    }

    /** Whether the bouncy screen/list animations should be skipped. */
    public static boolean isReduceMotion(@NonNull Context context) {
        return get(context).getBoolean(KEY_REDUCE_MOTION, false);
    }

    public static void setReduceMotion(@NonNull Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply();
    }

    /** Whether a snapshot should be saved automatically before a sensitive key is changed. */
    public static boolean isAutoBackup(@NonNull Context context) {
        return get(context).getBoolean(KEY_AUTO_BACKUP, true);
    }

    public static void setAutoBackup(@NonNull Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply();
    }

    /**
     * The ADB host this device's daemon was last reached on, or {@code null} when it was never
     * recorded. Remembering it lets a reconnect skip mDNS discovery, which is the slow and flaky
     * part of connecting to a device that is already paired.
     */
    @Nullable
    public static String getAdbHost(@NonNull Context context) {
        return get(context).getString(KEY_ADB_HOST, null);
    }

    public static int getAdbPort(@NonNull Context context) {
        return get(context).getInt(KEY_ADB_PORT, -1);
    }

    /** Stores the endpoint a successful connect used, so the next one can try it first. */
    public static void setAdbEndpoint(@NonNull Context context, @Nullable String host, int port) {
        if (host == null || port <= 0) return;
        get(context).edit().putString(KEY_ADB_HOST, host).putInt(KEY_ADB_PORT, port).apply();
    }

    public static void clearAdbEndpoint(@NonNull Context context) {
        get(context).edit().remove(KEY_ADB_HOST).remove(KEY_ADB_PORT).apply();
    }

    /**
     * True once the app has paired with this device's ADB daemon.
     *
     * <p>This is what lets the app stop asking the user to connect again: a paired device keeps
     * trusting the app's key, so a later failure is a transient transport problem to retry, not a
     * reason to send the user back to the pairing dialog.
     */
    public static boolean isAdbPaired(@NonNull Context context) {
        return get(context).getBoolean(KEY_ADB_PAIRED, false);
    }

    public static void setAdbPaired(@NonNull Context context, boolean paired) {
        get(context).edit().putBoolean(KEY_ADB_PAIRED, paired).apply();
    }

    /** True once any privileged route (root, Shizuku or ADB) has worked on this device. */
    public static boolean isPrivilegeGrantedOnce(@NonNull Context context) {
        return get(context).getBoolean(KEY_PRIVILEGE_GRANTED_ONCE, false);
    }

    public static void setPrivilegeGrantedOnce(@NonNull Context context, boolean granted) {
        get(context).edit().putBoolean(KEY_PRIVILEGE_GRANTED_ONCE, granted).apply();
    }

    /**
     * True when the user declined Shizuku's permission prompt.
     *
     * <p>Remembered so the prompt is not shown again on every screen; the connect screen offers it
     * explicitly and clears this flag when it succeeds.
     */
    public static boolean isShizukuDenied(@NonNull Context context) {
        return get(context).getBoolean(KEY_SHIZUKU_DENIED, false);
    }

    public static void setShizukuDenied(@NonNull Context context, boolean denied) {
        get(context).edit().putBoolean(KEY_SHIZUKU_DENIED, denied).apply();
    }
}
