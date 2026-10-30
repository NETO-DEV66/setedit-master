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

import io.github.netodev66.setedit.data.Recipe;
import io.github.netodev66.setedit.ui.RecipeAdapter;
import io.github.netodev66.setedit.utils.ActionResult;
import io.github.netodev66.setedit.utils.SettingsUtils;

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
        EXECUTOR.submit(() -> {
            boolean allOk = true;
            for (Recipe.Change change : recipe.changes) {
                ActionResult result = SettingsUtils.update(appContext, change.table, change.key, change.value);
                if (!result.successful) allOk = false;
            }
            final boolean ok = allOk;
            runOnUiThread(() -> Toast.makeText(this,
                    ok ? getString(R.string.recipe_applied, recipe.title)
                            : getString(R.string.recipe_failed, recipe.title),
                    Toast.LENGTH_SHORT).show());
        });
    }
}
