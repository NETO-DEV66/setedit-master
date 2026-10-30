package io.github.netodev66.setedit.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
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
}
