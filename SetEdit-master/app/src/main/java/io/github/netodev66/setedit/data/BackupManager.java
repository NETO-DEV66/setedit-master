package io.github.netodev66.setedit.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.github.netodev66.setedit.SettingsType;
import io.github.netodev66.setedit.utils.ActionResult;
import io.github.netodev66.setedit.utils.SettingsUtils;

/**
 * Exports and restores the three settings tables as one JSON document.
 *
 * <p>The format is a top-level object with a {@code tables} member mapping each table name to its
 * key/value pairs, so a backup is human-readable and forward-compatible.
 */
public final class BackupManager {
    public static final int FORMAT_VERSION = 1;

    /** What a backup should contain. */
    public enum Scope {
        ALL, FAVORITES, SYSTEM, SECURE, GLOBAL
    }

    private BackupManager() {
    }

    private static final String[] TABLES = {
            SettingsType.SYSTEM_SETTINGS, SettingsType.SECURE_SETTINGS, SettingsType.GLOBAL_SETTINGS
    };

    /** Writes every settings table to {@code os}. */
    public static void export(@NonNull Context context, @NonNull OutputStream os) throws Exception {
        export(context, os, Scope.ALL);
    }

    /** Writes the tables selected by {@code scope} to {@code os}. */
    public static void export(@NonNull Context context, @NonNull OutputStream os,
                              @NonNull Scope scope) throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", FORMAT_VERSION);
        root.put("created", System.currentTimeMillis());
        JSONObject tables = new JSONObject();
        for (String table : tablesFor(scope)) {
            JSONObject entries = new JSONObject();
            if (scope == Scope.FAVORITES) {
                // Only the starred keys, resolved to their current value.
                for (String token : FavoritesStore.all(context)) {
                    String[] parts = FavoritesStore.split(token);
                    if (parts == null || !table.equals(parts[0])) continue;
                    String value = io.github.netodev66.setedit.ui.Tables.readValue(context,
                            io.github.netodev66.setedit.ui.Tables.tableTypeOf(table), parts[1]);
                    if (value != null) entries.put(parts[1], value);
                }
            } else {
                List<Pair<String, String>> items = SettingsUtils.readAll(context, table);
                if (items == null) continue;
                for (Pair<String, String> item : items) {
                    if (item.first == null) continue;
                    entries.put(item.first, item.second == null ? "" : item.second);
                }
            }
            tables.put(table, entries);
        }
        root.put("tables", tables);
        os.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    /** The tables a scope covers. */
    @NonNull
    private static String[] tablesFor(@NonNull Scope scope) {
        switch (scope) {
            case SYSTEM:
                return new String[]{SettingsType.SYSTEM_SETTINGS};
            case SECURE:
                return new String[]{SettingsType.SECURE_SETTINGS};
            case GLOBAL:
                return new String[]{SettingsType.GLOBAL_SETTINGS};
            default:
                return TABLES;
        }
    }

    /** How many keys a backup document contains, or {@code -1} when it cannot be parsed. */
    public static int countKeys(@NonNull String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject tables = root.optJSONObject("tables");
            if (tables == null) return -1;
            int count = 0;
            for (String table : TABLES) {
                JSONObject entries = tables.optJSONObject(table);
                if (entries != null) count += entries.length();
            }
            return count;
        } catch (Throwable th) {
            return -1;
        }
    }

    /** Applies every key in {@code json}. Returns how many succeeded. */
    public static int restore(@NonNull Context context, @NonNull String json) {
        int applied = 0;
        try {
            JSONObject root = new JSONObject(json);
            JSONObject tables = root.optJSONObject("tables");
            if (tables == null) return 0;
            for (String table : TABLES) {
                JSONObject entries = tables.optJSONObject(table);
                if (entries == null) continue;
                java.util.Iterator<String> keys = entries.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = entries.optString(key, "");
                    ActionResult result = SettingsUtils.update(context, table, key, value);
                    if (result.successful) applied++;
                }
            }
        } catch (Throwable ignored) {
        }
        return applied;
    }

    @Nullable
    public static String readAll(@NonNull java.io.InputStream is) {
        try {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (Throwable th) {
            return null;
        }
    }
}
