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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.utils.AppActions;
import io.github.netodev66.setedit.utils.AppCommands;
import io.github.netodev66.setedit.utils.AppActionResult;
import io.github.netodev66.setedit.utils.CompatChange;
import io.github.netodev66.setedit.utils.PrivilegedAccess;

/**
 * The platform-compat overrides one package carries.
 *
 * <p>These are the behaviour changes Android applies to an app as its target SDK moves forward;
 * {@code am compat} can turn one on or off for a single package. The list comes from
 * {@code dumpsys platform_compat}, the only way to read them back — there is no per-package query
 * command. A change id is an opaque number, so the row shows the platform's name for it, and the
 * screen offers a reset-all because a wrong override can make an app misbehave.
 */
public class AppCompatChangesActivity extends AppCompatActivity
        implements AppCompatAdapter.Listener {

    private static final String EXTRA_PACKAGE = "package";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-app-compat");
        thread.setDaemon(true);
        return thread;
    });

    @NonNull
    private String packageName = "";
    private AppCompatAdapter adapter;
    private View emptyView;
    private MaterialTextView emptyText;
    private LinearProgressIndicator progress;

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull String pkg) {
        return new Intent(context, AppCompatChangesActivity.class).putExtra(EXTRA_PACKAGE, pkg);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_compat);

        packageName = String.valueOf(getIntent().getStringExtra(EXTRA_PACKAGE));
        if (!AppCommands.isValidPackage(packageName)) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.app_compat_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setSubtitle(packageName);

        emptyView = findViewById(R.id.app_compat_empty);
        emptyText = findViewById(R.id.app_compat_empty_text);
        progress = findViewById(R.id.app_compat_progress);

        RecyclerView recycler = findViewById(R.id.app_compat_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(null);
        adapter = new AppCompatAdapter(this);
        recycler.setAdapter(adapter);

        MaterialButton reset = findViewById(R.id.app_compat_reset);
        reset.setOnClickListener(v -> confirmReset());

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        EXECUTOR.submit(() -> {
            List<CompatChange> changes = AppActions.compatChanges(packageName);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                adapter.submit(changes);
                boolean isEmpty = changes.isEmpty();
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                if (isEmpty) emptyText.setText(R.string.app_compat_empty);
            });
        });
    }

    @Override
    public void onToggle(@NonNull CompatChange change, boolean enabled) {
        if (!AppCommands.isValidCompatChange(change.id)) {
            load();
            return;
        }
        withPrivilege(() -> runChange(change, enabled));
    }

    private void runChange(@NonNull CompatChange change, boolean enabled) {
        progress.setVisibility(View.VISIBLE);
        EXECUTOR.submit(() -> {
            AppActionResult result = AppActions.setCompatChange(change.id, packageName, enabled);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                if (!result.successful) {
                    Toast.makeText(this, R.string.app_result_failed_plain, Toast.LENGTH_SHORT).show();
                }
                // Re-read either way: on failure the switch must show what the platform still holds.
                load();
            });
        });
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_compat_reset_all)
                .setMessage(R.string.app_compat_reset_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.app_compat_reset_all, (d, w) -> withPrivilege(() -> {
                    progress.setVisibility(View.VISIBLE);
                    EXECUTOR.submit(() -> {
                        AppActionResult result = AppActions.resetCompatChanges(packageName);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            progress.setVisibility(View.GONE);
                            Toast.makeText(this, result.successful
                                            ? R.string.app_compat_reset_done
                                            : R.string.app_result_failed_plain,
                                    Toast.LENGTH_SHORT).show();
                            load();
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
                // The first load ran without access, so re-read before acting on stale rows.
                load();
                action.run();
                return;
            }
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        });
    }
}
