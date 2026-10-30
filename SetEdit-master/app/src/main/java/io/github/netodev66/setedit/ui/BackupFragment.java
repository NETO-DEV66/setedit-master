package io.github.netodev66.setedit.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.BackupManager;
import io.github.netodev66.setedit.data.Recipe;

/** Exports and restores settings, and offers the built-in recipes. */
public class BackupFragment extends Fragment implements RecipeAdapter.Listener {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-backup");
        thread.setDaemon(true);
        return thread;
    });

    /** The scope chosen in the export dialog, applied once the user picks a file. */
    private BackupManager.Scope pendingScope = BackupManager.Scope.ALL;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri == null) return;
                exportTo(uri, pendingScope);
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri == null) return;
                readAndConfirm(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_backup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.backup_toolbar);
        toolbar.setTitle(R.string.backup_title);

        MaterialButton export = view.findViewById(R.id.backup_export);
        MaterialButton restore = view.findViewById(R.id.backup_import);
        export.setOnClickListener(v -> showExportScopeDialog());
        restore.setOnClickListener(v -> importLauncher.launch(new String[]{"application/json", "*/*"}));

        RecyclerView recipes = view.findViewById(R.id.backup_recipes);
        recipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recipes.setItemAnimator(null);
        recipes.setAdapter(new RecipeAdapter(requireContext(), this, Recipe.all(requireContext())));
        io.github.netodev66.util.UiUtils.playBouncyScreenEntrance(recipes);
        io.github.netodev66.util.UiUtils.attachPressBounce(export);
        io.github.netodev66.util.UiUtils.attachPressBounce(restore);
    }

    /** Asks what to include before opening the file picker. */
    private void showExportScopeDialog() {
        String[] labels = {
                getString(R.string.backup_scope_all),
                getString(R.string.backup_scope_favorites),
                getString(R.string.backup_scope_system),
                getString(R.string.backup_scope_secure),
                getString(R.string.backup_scope_global)
        };
        BackupManager.Scope[] scopes = {
                BackupManager.Scope.ALL,
                BackupManager.Scope.FAVORITES,
                BackupManager.Scope.SYSTEM,
                BackupManager.Scope.SECURE,
                BackupManager.Scope.GLOBAL
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_export_scope_title)
                .setItems(labels, (dialog, which) -> {
                    pendingScope = scopes[which];
                    exportLauncher.launch(fileName());
                })
                .show();
    }

    private String fileName() {
        return "SetEdit-backup-" + System.currentTimeMillis() + ".json";
    }

    private void exportTo(@NonNull android.net.Uri uri, @NonNull BackupManager.Scope scope) {
        Context appContext = requireContext().getApplicationContext();
        Toast.makeText(requireContext(), R.string.backup_exporting, Toast.LENGTH_SHORT).show();
        EXECUTOR.submit(() -> {
            try (OutputStream os = appContext.getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new java.io.IOException();
                BackupManager.export(appContext, os, scope);
                View view = getView();
                if (view != null) {
                    view.post(() -> Toast.makeText(requireContext(), R.string.backup_exported,
                            Toast.LENGTH_SHORT).show());
                }
            } catch (Throwable th) {
                View view = getView();
                if (view != null) {
                    view.post(() -> Toast.makeText(requireContext(), R.string.failed,
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void readAndConfirm(@NonNull android.net.Uri uri) {
        Context appContext = requireContext().getApplicationContext();
        EXECUTOR.submit(() -> {
            String json;
            try (InputStream is = appContext.getContentResolver().openInputStream(uri)) {
                json = is == null ? null : BackupManager.readAll(is);
            } catch (Throwable th) {
                json = null;
            }
            final String content = json;
            int count = content == null ? -1 : BackupManager.countKeys(content);
            View view = getView();
            if (view == null) return;
            view.post(() -> {
                if (count < 0) {
                    Toast.makeText(requireContext(), R.string.backup_import_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.backup_import_confirm_title)
                        .setMessage(getString(R.string.backup_import_confirm_body, count))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.backup_restore, (d, w) -> restore(content, count))
                        .show();
            });
        });
    }

    private void restore(@NonNull String json, int total) {
        Context appContext = requireContext().getApplicationContext();
        EXECUTOR.submit(() -> {
            int applied = BackupManager.restore(appContext, json);
            View view = getView();
            if (view != null) {
                view.post(() -> Toast.makeText(requireContext(),
                        getString(R.string.backup_import_done, applied, total),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onApply(@NonNull Recipe recipe) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recipe_confirm_title)
                .setMessage(getString(R.string.recipe_confirm_body, recipe.title(requireContext()),
                        recipe.description(requireContext())))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.recipe_apply, (d, w) -> applyRecipe(recipe))
                .show();
    }

    private void applyRecipe(@NonNull Recipe recipe) {
        Context appContext = requireContext().getApplicationContext();
        EXECUTOR.submit(() -> {
            boolean allOk = true;
            for (Recipe.Change change : recipe.changes) {
                io.github.netodev66.setedit.utils.ActionResult result =
                        io.github.netodev66.setedit.utils.SettingsUtils.update(
                                appContext, change.table, change.key, change.value);
                if (!result.successful) allOk = false;
            }
            final boolean ok = allOk;
            View view = getView();
            if (view != null) {
                view.post(() -> Toast.makeText(requireContext(),
                        ok ? getString(R.string.recipe_applied, recipe.title(requireContext()))
                                : getString(R.string.recipe_failed, recipe.title(requireContext())),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }
}
