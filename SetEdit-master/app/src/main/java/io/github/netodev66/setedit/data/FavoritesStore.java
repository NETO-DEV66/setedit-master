package io.github.netodev66.setedit.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.github.netodev66.setedit.SettingsType;

/**
 * Remembers which keys the user starred.
 *
 * <p>A favorite is stored as {@code table\u0000key} so the same key name in two different tables
 * stays distinct. The set is kept in memory and mirrored to {@code SharedPreferences}.
 */
public final class FavoritesStore {
    private static final String FILE = "setedit_favorites";
    private static final String KEY_SET = "favorites";

    private FavoritesStore() {
    }

    private static String token(@NonNull String table, @NonNull String key) {
        return table + "\u0000" + key;
    }

    @NonNull
    public static Set<String> all(@NonNull Context context) {
        Set<String> stored = context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getStringSet(KEY_SET, Collections.emptySet());
        return stored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stored);
    }

    public static boolean isFavorite(@NonNull Context context, @NonNull String table, @NonNull String key) {
        return all(context).contains(token(table, key));
    }

    public static void add(@NonNull Context context, @NonNull String table, @NonNull String key) {
        Set<String> set = all(context);
        set.add(token(table, key));
        persist(context, set);
    }

    public static void remove(@NonNull Context context, @NonNull String table, @NonNull String key) {
        Set<String> set = all(context);
        set.remove(token(table, key));
        persist(context, set);
    }

    /** Flips the favorite state and returns the new state. */
    public static boolean toggle(@NonNull Context context, @NonNull String table, @NonNull String key) {
        if (isFavorite(context, table, key)) {
            remove(context, table, key);
            return false;
        }
        add(context, table, key);
        return true;
    }

    private static void persist(@NonNull Context context, @NonNull Set<String> set) {
        context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SET, new LinkedHashSet<>(set))
                .apply();
        io.github.netodev66.setedit.widget.SetEditWidgetProvider.refreshAll(context);
    }

    /** Splits a stored token back into its table and key, or {@code null} when malformed. */
    public static String[] split(@NonNull String token) {
        int index = token.indexOf('\u0000');
        if (index <= 0 || index >= token.length() - 1) return null;
        return new String[]{token.substring(0, index), token.substring(index + 1)};
    }

    public static boolean isSettingsTable(@NonNull String table) {
        return SettingsType.SYSTEM_SETTINGS.equals(table)
                || SettingsType.SECURE_SETTINGS.equals(table)
                || SettingsType.GLOBAL_SETTINGS.equals(table);
    }
}
