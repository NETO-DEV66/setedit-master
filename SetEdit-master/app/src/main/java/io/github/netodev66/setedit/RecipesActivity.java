package io.github.netodev66.setedit;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.EditorUtils;
import io.github.netodev66.setedit.data.Recipe;
import io.github.netodev66.setedit.ui.RecipeAdapter;
import io.github.netodev66.setedit.ui.Tables;
import io.github.netodev66.setedit.utils.ActionResult;
import io.github.netodev66.setedit.utils.SettingsUtils;
import io.github.netodev66.setedit.utils.ShellExecutor;

/** The full-screen recipes list, opened from the dashboard. */
public class RecipesActivity extends AppCompatActivity implements RecipeAdapter.Listener {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-recipes");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipes);
        MaterialToolbar toolbar = findViewById(R.id.recipes_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recycler = findViewById(R.id.recipes_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new RecipeAdapter(this, Recipe.all()));
    }

    @Override
    public void onApply(@NonNull Recipe recipe) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recipe_confirm_title)
                .setMessage(getString(R.string.recipe_confirm_body, recipe.title, recipe.description))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.recipe_apply, (d, w) -> apply(recipe))
                .show();
    }

    private void apply(@NonNull Recipe recipe) {
        Context appContext = getApplicationContext();
        if (!ShellExecutor.isAvailable() && !EditorUtils.hasSecureWrite(appContext)) {
            Toast.makeText(this, R.string.recipe_needs_access, Toast.LENGTH_LONG).show();
            return;
        }
        EXECUTOR.submit(() -> {
            boolean allOk = true;
            for (Recipe.Change change : recipe.changes) {
                ActionResult result = SettingsUtils.update(appContext, change.table, change.key, change.value);
                if (!result.successful || !matches(appContext, change)) allOk = false;
            }
            final boolean ok = allOk;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(this,
                        ok ? getString(R.string.recipe_applied, recipe.title)
                                : getString(R.string.recipe_failed, recipe.title),
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * Reads the key back and checks it holds the value we wrote.
     *
     * <p>{@code settings put} reports success even when the row was written to a table the
     * platform does not read, so a successful write is not proof the recipe took effect. Comparing
     * the stored value is what turns a silent no-op into an honest failure message.
     */
    private static boolean matches(@NonNull Context context, @NonNull Recipe.Change change) {
        String stored = Tables.readValue(context, Tables.tableTypeOf(change.table), change.key);
        if (stored == null) return false;
        try {
            return Double.parseDouble(stored.trim()) == Double.parseDouble(change.value.trim());
        } catch (NumberFormatException ignored) {
            return stored.trim().equalsIgnoreCase(change.value.trim());
        }
    }
}
