package io.github.netodev66.setedit.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Records the previous value of every key the user edits, newest first, so a change can be undone.
 *
 * <p>Only the previous value is kept — that is all an undo needs. History is capped per key so the
 * store cannot grow without bound.
 */
public final class HistoryStore {
    private static final String FILE = "setedit_history";
    private static final String KEY_JSON = "history";
    private static final int MAX_ENTRIES = 300;

    private HistoryStore() {
    }

    /** One past edit: which table and key changed, and what the value was before. */
    public static final class Entry {
        @NonNull
        public final String table;
        @NonNull
        public final String key;
        @Nullable
        public final String oldValue;
        @Nullable
        public final String newValue;
        public final long timestamp;

        Entry(@NonNull String table, @NonNull String key, @Nullable String oldValue,
              @Nullable String newValue, long timestamp) {
            this.table = table;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = timestamp;
        }
    }

    public static void record(@NonNull Context context, @NonNull String table, @NonNull String key,
                              @Nullable String oldValue, @Nullable String newValue) {
        List<Entry> entries = load(context);
        // Drop an older entry for the same key so the list reads as a set of distinct keys.
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.table.equals(table) && entry.key.equals(key)) {
                entries.remove(i);
            }
        }
        entries.add(0, new Entry(table, key, oldValue, newValue, System.currentTimeMillis()));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
        save(context, entries);
    }

    /** The most recent recorded change for a key, or {@code null} when there is none. */
    @Nullable
    public static Entry latestFor(@NonNull Context context, @NonNull String table, @NonNull String key) {
        for (Entry entry : load(context)) {
            if (entry.table.equals(table) && entry.key.equals(key)) return entry;
        }
        return null;
    }

    @NonNull
    public static List<Entry> load(@NonNull Context context) {
        String raw = prefs(context).getString(KEY_JSON, null);
        List<Entry> entries = new ArrayList<>();
        if (raw == null) return entries;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                entries.add(new Entry(
                        object.optString("table"),
                        object.optString("key"),
                        object.isNull("old") ? null : object.optString("old", null),
                        object.isNull("new") ? null : object.optString("new", null),
                        object.optLong("ts", 0L)));
            }
        } catch (Throwable ignored) {
        }
        return entries;
    }

    public static void clear(@NonNull Context context) {
        prefs(context).edit().remove(KEY_JSON).apply();
    }

    private static void save(@NonNull Context context, @NonNull List<Entry> entries) {
        try {
            JSONArray array = new JSONArray();
            for (Entry entry : entries) {
                JSONObject object = new JSONObject();
                object.put("table", entry.table);
                object.put("key", entry.key);
                object.put("old", entry.oldValue == null ? JSONObject.NULL : entry.oldValue);
                object.put("new", entry.newValue == null ? JSONObject.NULL : entry.newValue);
                object.put("ts", entry.timestamp);
                array.put(object);
            }
            prefs(context).edit().putString(KEY_JSON, array.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
