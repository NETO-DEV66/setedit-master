package io.github.netodev66.setedit.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.SettingsType;

/**
 * Persists the recipes the user creates.
 *
 * <p>Each recipe is stored as JSON: a name, an optional description, and the list of table/key/value
 * changes it applies. The format matches what the "new recipe" dialog builds, so a recipe created on
 * one device survives a reinstall that restores the prefs file.
 */
public final class CustomRecipeStore {
    private static final String FILE = "setedit_custom_recipes";
    private static final String KEY_JSON = "recipes";

    private CustomRecipeStore() {
    }

    @NonNull
    public static List<Recipe> all(@NonNull Context context) {
        List<Recipe> recipes = new ArrayList<>();
        String raw = prefs(context).getString(KEY_JSON, null);
        if (raw == null) return recipes;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                Recipe recipe = fromJson(object);
                if (recipe != null) recipes.add(recipe);
            }
        } catch (Throwable ignored) {
        }
        return recipes;
    }

    /** Adds a recipe and returns it, or {@code null} when it had no usable changes. */
    @Nullable
    public static Recipe add(@NonNull Context context, @NonNull String title,
                             @NonNull String description, @NonNull List<Recipe.Change> changes) {
        if (changes.isEmpty()) return null;
        List<Recipe> existing = all(context);
        String id = newId(existing);
        Recipe recipe = Recipe.custom(id, title, description, changes);
        existing.add(recipe);
        save(context, existing);
        return recipe;
    }

    /** A timestamped id, bumped until it is unique so two quick adds cannot collide. */
    @NonNull
    private static String newId(@NonNull List<Recipe> existing) {
        long stamp = System.currentTimeMillis();
        String id = "custom_" + stamp;
        int suffix = 1;
        while (containsId(existing, id)) {
            id = "custom_" + stamp + "_" + suffix++;
        }
        return id;
    }

    private static boolean containsId(@NonNull List<Recipe> recipes, @NonNull String id) {
        for (Recipe recipe : recipes) {
            if (recipe.id.equals(id)) return true;
        }
        return false;
    }

    /** Removes the recipe with {@code id}; returns whether anything was removed. */
    public static boolean remove(@NonNull Context context, @NonNull String id) {
        List<Recipe> existing = all(context);
        boolean removed = false;
        for (int i = existing.size() - 1; i >= 0; i--) {
            if (existing.get(i).id.equals(id)) {
                existing.remove(i);
                removed = true;
            }
        }
        if (removed) save(context, existing);
        return removed;
    }

    private static void save(@NonNull Context context, @NonNull List<Recipe> recipes) {
        try {
            JSONArray array = new JSONArray();
            for (Recipe recipe : recipes) {
                JSONObject object = new JSONObject();
                object.put("id", recipe.id);
                object.put("title", recipe.title(context));
                object.put("description", recipe.description(context));
                JSONArray changes = new JSONArray();
                for (Recipe.Change change : recipe.changes) {
                    JSONObject entry = new JSONObject();
                    entry.put("table", change.table);
                    entry.put("key", change.key);
                    entry.put("value", change.value);
                    changes.put(entry);
                }
                object.put("changes", changes);
                array.put(object);
            }
            prefs(context).edit().putString(KEY_JSON, array.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private static Recipe fromJson(@NonNull JSONObject object) {
        String id = object.optString("id", null);
        String title = object.optString("title", null);
        if (id == null || title == null || title.isEmpty()) return null;
        String description = object.optString("description", "");
        JSONArray array = object.optJSONArray("changes");
        if (array == null || array.length() == 0) return null;
        List<Recipe.Change> changes = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.optJSONObject(i);
            if (entry == null) continue;
            String table = entry.optString("table", SettingsType.SYSTEM_SETTINGS);
            String key = entry.optString("key", null);
            String value = entry.optString("value", "");
            if (key == null || key.isEmpty()) continue;
            changes.add(new Recipe.Change(normalizeTable(table), key, value));
        }
        if (changes.isEmpty()) return null;
        return Recipe.custom(id, title, description, changes);
    }

    /** Clamps a stored table name to one of the three editable settings tables. */
    @NonNull
    private static String normalizeTable(@Nullable String table) {
        if (SettingsType.SECURE_SETTINGS.equals(table)) return SettingsType.SECURE_SETTINGS;
        if (SettingsType.GLOBAL_SETTINGS.equals(table)) return SettingsType.GLOBAL_SETTINGS;
        return SettingsType.SYSTEM_SETTINGS;
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
