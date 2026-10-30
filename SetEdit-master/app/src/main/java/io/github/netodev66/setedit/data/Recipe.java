package io.github.netodev66.setedit.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.SettingsType;

/**
 * A one-tap tweak: a small set of settings keys with the values that produce a known result.
 *
 * <p>Recipes only ever touch the {@code system}/{@code secure}/{@code global} tables through the
 * same write path the editor uses, so they work with root, Shizuku or the one-time ADB connection.
 *
 * <p>A recipe is either built in — its title and description come from string resources so they
 * translate with the rest of the app — or user created, in which case it carries its own text and is
 * stored in {@link CustomRecipeStore}.
 */
public final class Recipe {
    /** The category a recipe belongs to, used to group the list. */
    public enum Category {
        ANIMATIONS(R.string.recipe_category_animations),
        DISPLAY(R.string.recipe_category_display),
        TOUCH(R.string.recipe_category_touch),
        ACCESSIBILITY(R.string.recipe_category_accessibility),
        CUSTOM(R.string.recipe_category_custom);

        @StringRes
        public final int labelRes;

        Category(@StringRes int labelRes) {
            this.labelRes = labelRes;
        }
    }

    @NonNull
    public final String id;
    @StringRes
    private final int titleRes;
    @StringRes
    private final int descriptionRes;
    @Nullable
    private final String customTitle;
    @Nullable
    private final String customDescription;
    @NonNull
    public final String icon;
    @NonNull
    public final Category category;
    @NonNull
    public final List<Change> changes;

    private Recipe(@NonNull String id, @StringRes int titleRes, @StringRes int descriptionRes,
                   @Nullable String customTitle, @Nullable String customDescription,
                   @NonNull String icon, @NonNull Category category, @NonNull List<Change> changes) {
        this.id = id;
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.customTitle = customTitle;
        this.customDescription = customDescription;
        this.icon = icon;
        this.category = category;
        this.changes = changes;
    }

    /** A built-in recipe whose title and description come from string resources. */
    @NonNull
    public static Recipe builtIn(@NonNull String id, @StringRes int titleRes,
                                 @StringRes int descriptionRes, @NonNull String icon,
                                 @NonNull Category category, @NonNull List<Change> changes) {
        return new Recipe(id, titleRes, descriptionRes, null, null, icon, category, changes);
    }

    /** A user-created recipe that carries its own title and description. */
    @NonNull
    public static Recipe custom(@NonNull String id, @NonNull String title,
                                @NonNull String description, @NonNull List<Change> changes) {
        return new Recipe(id, 0, 0, title, description, "custom", Category.CUSTOM, changes);
    }

    /** Resolves the title, preferring the user text for a custom recipe. */
    @NonNull
    public String title(@NonNull Context context) {
        if (customTitle != null) return customTitle;
        return titleRes == 0 ? "" : context.getString(titleRes);
    }

    /** Resolves the description, preferring the user text for a custom recipe. */
    @NonNull
    public String description(@NonNull Context context) {
        if (customDescription != null) return customDescription;
        return descriptionRes == 0 ? "" : context.getString(descriptionRes);
    }

    /** Whether this recipe was created by the user and can be deleted. */
    public boolean isCustom() {
        return category == Category.CUSTOM;
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
    public static List<Recipe> builtIn() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(Recipe.builtIn("fast_animations", R.string.recipe_fast_animations_title,
                R.string.recipe_fast_animations_desc, "speed", Category.ANIMATIONS,
                changes(global("window_animation_scale", "0.5"),
                        global("transition_animation_scale", "0.5"),
                        global("animator_duration_scale", "0.5"))));
        recipes.add(Recipe.builtIn("no_animations", R.string.recipe_no_animations_title,
                R.string.recipe_no_animations_desc, "speed_off", Category.ANIMATIONS,
                changes(global("window_animation_scale", "0.0"),
                        global("transition_animation_scale", "0.0"),
                        global("animator_duration_scale", "0.0"))));
        recipes.add(Recipe.builtIn("default_animations", R.string.recipe_default_animations_title,
                R.string.recipe_default_animations_desc, "restore", Category.ANIMATIONS,
                changes(global("window_animation_scale", "1.0"),
                        global("transition_animation_scale", "1.0"),
                        global("animator_duration_scale", "1.0"))));
        recipes.add(Recipe.builtIn("show_touches", R.string.recipe_show_touches_title,
                R.string.recipe_show_touches_desc, "touch", Category.TOUCH,
                changes(sys("show_touches", "1"), sys("pointer_location", "1"))));
        recipes.add(Recipe.builtIn("hide_touches", R.string.recipe_hide_touches_title,
                R.string.recipe_hide_touches_desc, "touch_off", Category.TOUCH,
                changes(sys("show_touches", "0"), sys("pointer_location", "0"))));
        recipes.add(Recipe.builtIn("stay_awake", R.string.recipe_stay_awake_title,
                R.string.recipe_stay_awake_desc, "screen", Category.DISPLAY,
                changes(global("stay_on_while_plugged_in", "7"))));
        recipes.add(Recipe.builtIn("screen_timeout_5m", R.string.recipe_screen_timeout_5m_title,
                R.string.recipe_screen_timeout_5m_desc, "screen", Category.DISPLAY,
                changes(sys("screen_off_timeout", "300000"))));
        recipes.add(Recipe.builtIn("screen_timeout_30s", R.string.recipe_screen_timeout_30s_title,
                R.string.recipe_screen_timeout_30s_desc, "screen", Category.DISPLAY,
                changes(sys("screen_off_timeout", "30000"))));
        recipes.add(Recipe.builtIn("rotation_on", R.string.recipe_rotation_on_title,
                R.string.recipe_rotation_on_desc, "rotate", Category.DISPLAY,
                changes(sys("accelerometer_rotation", "1"))));
        recipes.add(Recipe.builtIn("rotation_off", R.string.recipe_rotation_off_title,
                R.string.recipe_rotation_off_desc, "rotate_off", Category.DISPLAY,
                changes(sys("accelerometer_rotation", "0"))));
        recipes.add(Recipe.builtIn("font_scale", R.string.recipe_font_scale_title,
                R.string.recipe_font_scale_desc, "text", Category.ACCESSIBILITY,
                changes(sys("font_scale", "1.0"))));
        return recipes;
    }

    /** The built-in recipes followed by the user's custom ones. */
    @NonNull
    public static List<Recipe> all(@NonNull Context context) {
        List<Recipe> recipes = new ArrayList<>(builtIn());
        recipes.addAll(CustomRecipeStore.all(context));
        return recipes;
    }

    /** Kept for callers that only want the built-in set. */
    @NonNull
    public static List<Recipe> all() {
        return builtIn();
    }

    @Nullable
    public static Recipe byId(@NonNull String id) {
        for (Recipe recipe : builtIn()) {
            if (recipe.id.equals(id)) return recipe;
        }
        return null;
    }
}
