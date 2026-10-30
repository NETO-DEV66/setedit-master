package io.github.netodev66.setedit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.netodev66.setedit.SettingsType;

/** Round-trips custom recipes through storage, including the id-collision and cleanup paths. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CustomRecipeStoreTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        for (Recipe recipe : CustomRecipeStore.all(context)) {
            CustomRecipeStore.remove(context, recipe.id);
        }
    }

    private static List<Recipe.Change> oneChange() {
        List<Recipe.Change> changes = new ArrayList<>();
        changes.add(new Recipe.Change(SettingsType.GLOBAL_SETTINGS, "window_animation_scale", "0.5"));
        return changes;
    }

    @Test
    public void addThenReadBackKeepsTitleAndChanges() {
        Recipe added = CustomRecipeStore.add(context, "My tweak", "Does a thing", oneChange());
        assertNotNull(added);
        List<Recipe> stored = CustomRecipeStore.all(context);
        assertEquals(1, stored.size());
        Recipe recipe = stored.get(0);
        assertEquals("My tweak", recipe.title(context));
        assertEquals("Does a thing", recipe.description(context));
        assertEquals(1, recipe.changes.size());
        assertEquals("window_animation_scale", recipe.changes.get(0).key);
        assertEquals("0.5", recipe.changes.get(0).value);
        assertEquals(SettingsType.GLOBAL_SETTINGS, recipe.changes.get(0).table);
    }

    @Test
    public void twoQuickAddsGetDistinctIds() {
        Recipe first = CustomRecipeStore.add(context, "One", "", oneChange());
        Recipe second = CustomRecipeStore.add(context, "Two", "", oneChange());
        assertNotNull(first);
        assertNotNull(second);
        assertFalse("ids must differ even within the same millisecond",
                first.id.equals(second.id));
        assertEquals(2, CustomRecipeStore.all(context).size());
    }

    @Test
    public void addWithNoChangesIsRejected() {
        assertNull(CustomRecipeStore.add(context, "Empty", "", Collections.emptyList()));
        assertTrue(CustomRecipeStore.all(context).isEmpty());
    }

    @Test
    public void removeDropsOnlyTheNamedRecipe() {
        Recipe keep = CustomRecipeStore.add(context, "Keep", "", oneChange());
        Recipe drop = CustomRecipeStore.add(context, "Drop", "", oneChange());
        assertNotNull(keep);
        assertNotNull(drop);
        assertTrue(CustomRecipeStore.remove(context, drop.id));
        List<Recipe> remaining = CustomRecipeStore.all(context);
        assertEquals(1, remaining.size());
        assertEquals("Keep", remaining.get(0).title(context));
        assertFalse(CustomRecipeStore.remove(context, "does_not_exist"));
    }

    @Test
    public void unknownTableIsClampedToSystem() {
        // A corrupt or hand-edited prefs file must not let a custom recipe write elsewhere.
        context.getSharedPreferences("setedit_custom_recipes", Context.MODE_PRIVATE)
                .edit()
                .putString("recipes", "[{\"id\":\"custom_x\",\"title\":\"T\",\"description\":\"\","
                        + "\"changes\":[{\"table\":\"properties\",\"key\":\"k\",\"value\":\"v\"}]}]")
                .commit();
        List<Recipe> stored = CustomRecipeStore.all(context);
        assertEquals(1, stored.size());
        assertEquals(SettingsType.SYSTEM_SETTINGS, stored.get(0).changes.get(0).table);
    }
}
