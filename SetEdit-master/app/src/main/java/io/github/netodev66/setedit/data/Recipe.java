package io.github.netodev66.setedit.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.SettingsType;

/**
 * A one-tap tweak: a small set of settings keys with the values that produce a known result.
 *
 * <p>Recipes only ever touch the {@code system}/{@code secure}/{@code global} tables through the
 * same write path the editor uses, so they work with root, Shizuku or the one-time ADB connection.
 */
public final class Recipe {
    @NonNull
    public final String id;
    @NonNull
    public final String title;
    @NonNull
    public final String description;
    @NonNull
    public final String icon;
    @NonNull
    public final List<Change> changes;

    public Recipe(@NonNull String id, @NonNull String title, @NonNull String description,
                  @NonNull String icon, @NonNull List<Change> changes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.changes = changes;
    }

    /** One key/value pair a recipe sets. */
    public static final class Change {
        @SettingsType
        @NonNull
        public final String table;
        @NonNull
        public final String key;
        @NonNull
        public final String value;

        public Change(@SettingsType @NonNull String table, @NonNull String key, @NonNull String value) {
            this.table = table;
            this.key = key;
            this.value = value;
        }
    }

    private static Change sys(String key, String value) {
        return new Change(SettingsType.SYSTEM_SETTINGS, key, value);
    }

    private static Change secure(String key, String value) {
        return new Change(SettingsType.SECURE_SETTINGS, key, value);
    }

    private static Change global(String key, String value) {
        return new Change(SettingsType.GLOBAL_SETTINGS, key, value);
    }

    private static List<Change> changes(Change... items) {
        List<Change> list = new ArrayList<>(items.length);
        for (Change item : items) list.add(item);
        return list;
    }

    /**
     * The built-in recipes. Values are the documented defaults for each feature, so applying a
     * recipe always produces a predictable, reversible result.
     *
     * <p>Each change must name the table the platform actually reads the key from — the animation
     * scales live in {@code global} ({@code Settings.Global.WINDOW_ANIMATION_SCALE} and friends),
     * not {@code system}. Writing a key to the wrong table silently succeeds and does nothing,
     * because {@code settings put} creates the row without complaint.
     */
    @NonNull
    public static List<Recipe> all() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(new Recipe("fast_animations", "Fast animations",
                "Set window, transition and animator scales to 0.5x for snappier transitions.",
                "speed",
                changes(global("window_animation_scale", "0.5"),
                        global("transition_animation_scale", "0.5"),
                        global("animator_duration_scale", "0.5"))));
        recipes.add(new Recipe("no_animations", "Turn off animations",
                "Disable all system animations. Useful for older or slower devices.",
                "speed",
                changes(global("window_animation_scale", "0.0"),
                        global("transition_animation_scale", "0.0"),
                        global("animator_duration_scale", "0.0"))));
        recipes.add(new Recipe("default_animations", "Default animations",
                "Restore the standard 1x animation scales.",
                "restore",
                changes(global("window_animation_scale", "1.0"),
                        global("transition_animation_scale", "1.0"),
                        global("animator_duration_scale", "1.0"))));
        recipes.add(new Recipe("show_touches", "Show touch points",
                "Draw a dot wherever you touch the screen. Handy for demos and recordings.",
                "touch",
                changes(sys("show_touches", "1"), sys("pointer_location", "1"))));
        recipes.add(new Recipe("hide_touches", "Hide touch points",
                "Stop drawing touch dots and pointer location.",
                "touch",
                changes(sys("show_touches", "0"), sys("pointer_location", "0"))));
        recipes.add(new Recipe("stay_awake", "Keep screen on while charging",
                "Keep the display awake whenever the device is plugged in.",
                "screen",
                changes(global("stay_on_while_plugged_in", "7"))));
        recipes.add(new Recipe("screen_timeout_5m", "Screen timeout: 5 minutes",
                "Set the screen-off timeout to five minutes.",
                "screen",
                changes(sys("screen_off_timeout", "300000"))));
        recipes.add(new Recipe("screen_timeout_30s", "Screen timeout: 30 seconds",
                "Set the screen-off timeout to thirty seconds.",
                "screen",
                changes(sys("screen_off_timeout", "30000"))));
        recipes.add(new Recipe("rotation_on", "Auto-rotate on",
                "Enable automatic screen rotation.",
                "rotate",
                changes(sys("accelerometer_rotation", "1"))));
        recipes.add(new Recipe("rotation_off", "Auto-rotate off",
                "Lock the screen to its current orientation.",
                "rotate",
                changes(sys("accelerometer_rotation", "0"))));
        recipes.add(new Recipe("high_contrast_off", "Reset accessibility text scaling",
                "Return the system font scale to 1.0.",
                "text",
                changes(sys("font_scale", "1.0"))));
        return recipes;
    }

    @Nullable
    public static Recipe byId(@NonNull String id) {
        for (Recipe recipe : all()) {
            if (recipe.id.equals(id)) return recipe;
        }
        return null;
    }
}
