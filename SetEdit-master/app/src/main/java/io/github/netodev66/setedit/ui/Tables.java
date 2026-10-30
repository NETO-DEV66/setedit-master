package io.github.netodev66.setedit.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.SettingsType;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.utils.SettingsUtils;

/** Small helpers shared by the screens that read the settings tables. */
public final class Tables {
    private Tables() {
    }

    /** One key/value pair together with the table it came from. */
    public static final class Hit {
        @TableTypeInt
        public final int tableType;
        @NonNull
        public final String key;
        @NonNull
        public final String value;

        public Hit(@TableTypeInt int tableType, @NonNull String key, @NonNull String value) {
            this.tableType = tableType;
            this.key = key;
            this.value = value;
        }
    }

    public static boolean isSettingsTable(@TableTypeInt int tableType) {
        return tableType == TableTypeInt.TABLE_SYSTEM
                || tableType == TableTypeInt.TABLE_SECURE
                || tableType == TableTypeInt.TABLE_GLOBAL;
    }

    @SettingsType
    @NonNull
    public static String settingsTypeOf(@TableTypeInt int tableType) {
        switch (tableType) {
            case TableTypeInt.TABLE_SECURE:
                return SettingsType.SECURE_SETTINGS;
            case TableTypeInt.TABLE_GLOBAL:
                return SettingsType.GLOBAL_SETTINGS;
            default:
                return SettingsType.SYSTEM_SETTINGS;
        }
    }

    @TableTypeInt
    public static int tableTypeOf(@SettingsType @NonNull String settingsType) {
        switch (settingsType) {
            case SettingsType.SECURE_SETTINGS:
                return TableTypeInt.TABLE_SECURE;
            case SettingsType.GLOBAL_SETTINGS:
                return TableTypeInt.TABLE_GLOBAL;
            default:
                return TableTypeInt.TABLE_SYSTEM;
        }
    }

    /** Reads the current value of one key, or {@code null} when it is not present. */
    @Nullable
    public static String readValue(@NonNull Context context, @TableTypeInt int tableType,
                                   @NonNull String key) {
        if (!isSettingsTable(tableType)) return null;
        if (io.github.netodev66.setedit.utils.ShellExecutor.isAvailable()) {
            io.github.netodev66.setedit.utils.ShellExecutor.Result result =
                    io.github.netodev66.setedit.utils.ShellExecutor.exec(
                            "settings get " + settingsTypeOf(tableType) + " " + key);
            if (result.isSuccess() && !result.stdout.isEmpty()) {
                String value = result.stdout.get(0);
                return "null".equals(value) ? null : value;
            }
        }
        // No shell: fall back to the provider, which WRITE_SECURE_SETTINGS also lets us read.
        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://settings/" + settingsTypeOf(tableType)),
                new String[]{"value"}, "name = ?", new String[]{key}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Searches the system, secure and global tables for keys containing {@code query}. */
    @NonNull
    public static List<Hit> searchSettings(@NonNull Context context, @NonNull String query) {
        String needle = query.trim().toLowerCase(java.util.Locale.ROOT);
        List<Hit> hits = new ArrayList<>();
        if (needle.isEmpty()) return hits;
        int[] tables = {TableTypeInt.TABLE_SYSTEM, TableTypeInt.TABLE_SECURE, TableTypeInt.TABLE_GLOBAL};
        for (int tableType : tables) {
            List<Pair<String, String>> items =
                    SettingsUtils.readAll(context, settingsTypeOf(tableType));
            if (items == null) continue;
            for (Pair<String, String> item : items) {
                String key = item.first == null ? "" : item.first;
                if (key.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                    hits.add(new Hit(tableType, key, item.second == null ? "" : item.second));
                }
            }
        }
        return hits;
    }
}
