package io.github.netodev66.setedit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.netodev66.setedit.SettingsType;

/**
 * Guards the tables the recipes write to.
 *
 * <p>A key written to the wrong table still succeeds — {@code settings put} creates the row without
 * complaint — so the mistake is invisible at runtime and the recipe silently does nothing. These
 * tests pin each key to the table the platform actually reads it from.
 */
public class RecipeTest {

    /** Where AOSP reads each key from. Animation scales are global, not system. */
    private static final Map<String, String> KEY_TABLE = new HashMap<>();

    static {
        KEY_TABLE.put("window_animation_scale", SettingsType.GLOBAL_SETTINGS);
        KEY_TABLE.put("transition_animation_scale", SettingsType.GLOBAL_SETTINGS);
        KEY_TABLE.put("animator_duration_scale", SettingsType.GLOBAL_SETTINGS);
        KEY_TABLE.put("stay_on_while_plugged_in", SettingsType.GLOBAL_SETTINGS);
        KEY_TABLE.put("show_touches", SettingsType.SYSTEM_SETTINGS);
        KEY_TABLE.put("pointer_location", SettingsType.SYSTEM_SETTINGS);
        KEY_TABLE.put("screen_off_timeout", SettingsType.SYSTEM_SETTINGS);
        KEY_TABLE.put("accelerometer_rotation", SettingsType.SYSTEM_SETTINGS);
        KEY_TABLE.put("font_scale", SettingsType.SYSTEM_SETTINGS);
    }

    @Test
    public void animationScalesTargetGlobalNotSystem() {
        Recipe recipe = Recipe.byId("fast_animations");
        assertNotNull(recipe);
        for (Recipe.Change change : recipe.changes) {
            assertEquals("animation scale must live in global",
                    SettingsType.GLOBAL_SETTINGS, change.table);
        }
    }

    @Test
    public void everyChangeTargetsTheTableThePlatformReads() {
        for (Recipe recipe : Recipe.all()) {
            for (Recipe.Change change : recipe.changes) {
                String expected = KEY_TABLE.get(change.key);
                assertNotNull("no expected table recorded for " + change.key, expected);
                assertEquals(recipe.id + " writes " + change.key + " to the wrong table",
                        expected, change.table);
            }
        }
    }

    @Test
    public void idsAreUnique() {
        Set<String> seen = new HashSet<>();
        for (Recipe recipe : Recipe.all()) {
            assertTrue("duplicate recipe id " + recipe.id, seen.add(recipe.id));
        }
    }

    @Test
    public void everyRecipeHasAtLeastOneChange() {
        for (Recipe recipe : Recipe.all()) {
            assertFalse(recipe.id + " has no changes", recipe.changes.isEmpty());
        }
    }

    @Test
    public void animationRecipesUseExactScales() {
        Recipe off = Recipe.byId("no_animations");
        assertNotNull(off);
        for (Recipe.Change change : off.changes) {
            assertEquals("0.0", change.value);
        }
        Recipe defaults = Recipe.byId("default_animations");
        assertNotNull(defaults);
        for (Recipe.Change change : defaults.changes) {
            assertEquals("1.0", change.value);
        }
    }
}
