package io.github.netodev66.setedit;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.data.CustomRecipeStore;
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

    private RecipeAdapter adapter;
    private View emptyView;
    private MaterialTextView emptyTitle;
    @NonNull
    private String query = "";
    @Nullable
    private Recipe.Category categoryFilter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipes);
        MaterialToolbar toolbar = findViewById(R.id.recipes_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyView = findViewById(R.id.recipes_empty);
        emptyTitle = findViewById(R.id.recipes_empty_title);

        RecyclerView recycler = findViewById(R.id.recipes_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(null);
        adapter = new RecipeAdapter(this, this, Recipe.all(this));
        recycler.setAdapter(adapter);

        buildCategoryChips();

        TextInputEditText search = findViewById(R.id.recipes_search);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                query = s == null ? "" : s.toString();
                applyFilter();
            }
        });

        ExtendedFloatingActionButton newButton = findViewById(R.id.recipes_new);
        newButton.setOnClickListener(v -> showNewRecipeDialog());
        applyFilter();
    }

    /** Adds an "All" chip followed by one chip per category, skipping Custom when it is empty. */
    private void buildCategoryChips() {
        ChipGroup group = findViewById(R.id.recipes_categories);
        group.removeAllViews();
        addChip(group, getString(R.string.recipe_category_all), null, true);
        for (Recipe.Category category : Recipe.Category.values()) {
            if (category == Recipe.Category.CUSTOM && !hasCustomRecipes()) continue;
            addChip(group, getString(category.labelRes), category, false);
        }
    }

    private boolean hasCustomRecipes() {
        for (Recipe recipe : Recipe.all(this)) {
            if (recipe.isCustom()) return true;
        }
        return false;
    }

    private void addChip(@NonNull ChipGroup group, @NonNull String label,
                         @Nullable Recipe.Category category, boolean checked) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setOnClickListener(v -> {
            categoryFilter = category;
            applyFilter();
        });
        group.addView(chip);
    }

    private void applyFilter() {
        if (adapter == null) return;
        adapter.setFilter(query, categoryFilter);
        boolean isEmpty = adapter.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            emptyTitle.setText(query.isEmpty()
                    ? getString(R.string.empty_generic)
                    : getString(R.string.recipe_empty, query));
        }
    }

    private void showNewRecipeDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_new_recipe, null, false);
        TextInputEditText name = content.findViewById(R.id.new_recipe_name);
        TextInputEditText changesInput = content.findViewById(R.id.new_recipe_changes);
        MaterialAutoCompleteTextView table = content.findViewById(R.id.new_recipe_table);
        table.setText(getString(R.string.system_table), false);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recipe_new_title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.recipe_create, (dialog, which) -> {
                    String title = text(name);
                    String tableName = Tables.settingsTypeOf(tableTypeOf(table.getText().toString()));
                    List<Recipe.Change> changes = parseChanges(tableName, text(changesInput));
                    if (title.isEmpty() || changes.isEmpty()) {
                        Toast.makeText(this, R.string.recipe_invalid, Toast.LENGTH_LONG).show();
                        return;
                    }
                    CustomRecipeStore.add(this, title, "", changes);
                    Toast.makeText(this, R.string.recipe_created, Toast.LENGTH_SHORT).show();
                    refreshRecipes();
                })
                .show();
    }

    private void refreshRecipes() {
        adapter.submit(Recipe.all(this));
        buildCategoryChips();
        applyFilter();
    }

    /** Parses one {@code key=value} per line into changes for the chosen table. */
    @NonNull
    private List<Recipe.Change> parseChanges(@NonNull String table, @NonNull String text) {
        List<Recipe.Change> changes = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            int equals = trimmed.indexOf('=');
            if (equals <= 0) continue;
            String key = trimmed.substring(0, equals).trim();
            String value = trimmed.substring(equals + 1).trim();
            if (key.isEmpty()) continue;
            changes.add(new Recipe.Change(table, key, value));
        }
        return changes;
    }

    @TableTypeInt
    private int tableTypeOf(@NonNull String label) {
        if (label.equals(getString(R.string.secure_table))) return TableTypeInt.TABLE_SECURE;
        if (label.equals(getString(R.string.global_table))) return TableTypeInt.TABLE_GLOBAL;
        return TableTypeInt.TABLE_SYSTEM;
    }

    @NonNull
    private static String text(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString();
    }

    @Override
    public void onApply(@NonNull Recipe recipe) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recipe_confirm_title)
                .setMessage(getString(R.string.recipe_confirm_body, recipe.title(this),
                        recipe.description(this)))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.recipe_apply, (d, w) -> apply(recipe))
                .show();
    }

    @Override
    public void onLongClick(@NonNull Recipe recipe) {
        if (!recipe.isCustom()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recipe_delete_title)
                .setMessage(getString(R.string.recipe_delete_message, recipe.title(this)))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    CustomRecipeStore.remove(this, recipe.id);
                    Toast.makeText(this, R.string.recipe_deleted, Toast.LENGTH_SHORT).show();
                    refreshRecipes();
                })
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
                        ok ? getString(R.string.recipe_applied, recipe.title(this))
                                : getString(R.string.recipe_failed, recipe.title(this)),
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
