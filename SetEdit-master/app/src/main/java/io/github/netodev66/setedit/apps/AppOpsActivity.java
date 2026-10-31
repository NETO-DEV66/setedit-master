package io.github.netodev66.setedit.apps;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.utils.AppActions;
import io.github.netodev66.setedit.utils.AppCommands;
import io.github.netodev66.setedit.utils.AppOpCatalog;
import io.github.netodev66.setedit.utils.AppActionResult;
import io.github.netodev66.setedit.utils.PrivilegedAccess;

/**
 * Every AppOps operation for one package, with the mode currently in force.
 *
 * <p>The list is built from the bundled catalogue, not from the device: the shell cannot enumerate
 * ops. One {@code cmd appops get <pkg>} call returns the ops this package has moved away from their
 * default, and those values are overlaid on the catalogue. That keeps a load to a single shell
 * round trip instead of one per op.
 */
public class AppOpsActivity extends AppCompatActivity implements AppOpAdapter.Listener {

    private static final String EXTRA_PACKAGE = "package";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-app-ops");
        thread.setDaemon(true);
        return thread;
    });

    @NonNull
    private String packageName = "";
    /** Modes read from the device; ops absent here are still at their platform default. */
    @NonNull
    private final Map<String, String> setModes = new java.util.LinkedHashMap<>();
    private AppOpAdapter adapter;
    private View emptyView;
    private MaterialTextView emptyText;
    private MaterialTextView countView;
    private MaterialSwitch changedOnly;
    private LinearProgressIndicator progress;
    @NonNull
    private String query = "";

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull String pkg) {
        return new Intent(context, AppOpsActivity.class).putExtra(EXTRA_PACKAGE, pkg);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_ops);

        packageName = String.valueOf(getIntent().getStringExtra(EXTRA_PACKAGE));
        if (!AppCommands.isValidPackage(packageName)) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.app_ops_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setSubtitle(packageName);

        emptyView = findViewById(R.id.app_ops_empty);
        emptyText = findViewById(R.id.app_ops_empty_text);
        countView = findViewById(R.id.app_ops_count);
        changedOnly = findViewById(R.id.app_ops_changed_only);
        progress = findViewById(R.id.app_ops_progress);

        RecyclerView recycler = findViewById(R.id.app_ops_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(null);
        adapter = new AppOpAdapter(this);
        recycler.setAdapter(adapter);

        TextInputEditText search = findViewById(R.id.app_ops_search);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilter();
            }
        });
        changedOnly.setOnCheckedChangeListener((button, checked) -> applyFilter());

        MaterialButton reset = findViewById(R.id.app_ops_reset);
        reset.setOnClickListener(v -> confirmReset());

        load();
    }

    /** Reads the package's non-default ops, then paints the whole catalogue. */
    private void load() {
        progress.setVisibility(View.VISIBLE);
        EXECUTOR.submit(() -> {
            Map<String, String> modes = AppActions.appOpsSet(packageName);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                setModes.clear();
                setModes.putAll(modes);
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        List<AppOpAdapter.AppOpRow> rows = new ArrayList<>();
        for (AppOpCatalog.AppOp op : AppOpCatalog.OPS) {
            String mode = setModes.containsKey(op.name) ? setModes.get(op.name) : op.defaultMode;
            AppOpAdapter.AppOpRow row = new AppOpAdapter.AppOpRow(op.name, mode, op.defaultMode);
            if (changedOnly.isChecked() && !row.isChanged()) continue;
            if (!query.isEmpty() && !op.name.toLowerCase(Locale.ROOT).contains(query)) continue;
            rows.add(row);
        }
        adapter.submit(rows);
        countView.setText(getString(R.string.app_ops_count, rows.size()));
        boolean isEmpty = rows.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            emptyText.setText(getString(changedOnly.isChecked() || !query.isEmpty()
                    ? R.string.app_ops_empty
                    : R.string.app_ops_empty_none));
        }
    }

    @Override
    public void onOpClick(@NonNull AppOpAdapter.AppOpRow row) {
        if (!AppOpCatalog.isKnown(row.name)) return;
        String[] modes = {
                getString(R.string.app_appops_allow),
                getString(R.string.app_appops_ignore),
                getString(R.string.app_appops_deny),
                getString(R.string.app_appops_foreground),
                getString(R.string.app_appops_default),
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(row.name)
                .setItems(modes, (dialog, which) -> applyMode(row, AppCommands.OP_MODES.get(which)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyMode(@NonNull AppOpAdapter.AppOpRow row, @NonNull String mode) {
        withPrivilege(() -> {
            progress.setVisibility(View.VISIBLE);
            EXECUTOR.submit(() -> {
                AppActionResult result = AppActions.setAppOp(packageName, row.name, mode);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    progress.setVisibility(View.GONE);
                    if (result.successful) {
                        // Reflect the change without a second round trip; a reset-all rebuilds.
                        if (mode.equals(row.defaultMode)) {
                            setModes.remove(row.name);
                        } else {
                            setModes.put(row.name, mode);
                        }
                        applyFilter();
                    } else {
                        Toast.makeText(this, R.string.app_result_failed_plain,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_ops_reset_all)
                .setMessage(R.string.app_ops_reset_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.app_ops_reset_all, (d, w) -> withPrivilege(() -> {
                    progress.setVisibility(View.VISIBLE);
                    EXECUTOR.submit(() -> {
                        AppActionResult result = AppActions.resetAppOps(packageName);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            progress.setVisibility(View.GONE);
                            Toast.makeText(this, result.successful
                                            ? R.string.app_ops_reset_done
                                            : R.string.app_result_failed_plain,
                                    Toast.LENGTH_SHORT).show();
                            if (result.successful) load();
                        });
                    });
                }))
                .show();
    }

    /** Runs {@code action} once a privileged shell is known to be up, else offers to restore it. */
    private void withPrivilege(@NonNull Runnable action) {
        if (AppActions.isPrivileged()) {
            action.run();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        PrivilegedAccess.ensure(this, (ready, reason) -> {
            if (isFinishing() || isDestroyed()) return;
            progress.setVisibility(View.GONE);
            if (ready) {
                // The first load ran without access, so the list still shows every op at its
                // default. Re-read now that the shell is up, then run the action.
                load();
                action.run();
                return;
            }
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        });
    }
}
