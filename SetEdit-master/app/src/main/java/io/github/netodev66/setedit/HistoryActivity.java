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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.data.HistoryStore;
import io.github.netodev66.setedit.ui.HistoryAdapter;
import io.github.netodev66.setedit.utils.ActionResult;
import io.github.netodev66.setedit.utils.SettingsUtils;

/** The change history screen: every recorded edit, with the ability to undo one. */
public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.Listener {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-history");
        thread.setDaemon(true);
        return thread;
    });

    private HistoryAdapter adapter;
    private View emptyView;
    @Nullable
    private String tableFilter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        MaterialToolbar toolbar = findViewById(R.id.history_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyView = findViewById(R.id.history_empty);
        RecyclerView recycler = findViewById(R.id.history_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(null);
        adapter = new HistoryAdapter(this);
        recycler.setAdapter(adapter);

        buildFilters();
        findViewById(R.id.history_clear).setOnClickListener(v -> confirmClear());
        reload();
    }

    private void buildFilters() {
        ChipGroup group = findViewById(R.id.history_filters);
        group.removeAllViews();
        addChip(group, getString(R.string.history_filter_all), null, true);
        for (String table : new String[]{"system", "secure", "global"}) {
            if (!hasTable(table)) continue;
            addChip(group, table, table, false);
        }
    }

    private boolean hasTable(@NonNull String table) {
        for (HistoryStore.Entry entry : HistoryStore.load(this)) {
            if (entry.table.equals(table)) return true;
        }
        return false;
    }

    private void addChip(@NonNull ChipGroup group, @NonNull String label,
                         @Nullable String table, boolean checked) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setOnClickListener(v -> {
            tableFilter = table;
            reload();
        });
        group.addView(chip);
    }

    private void reload() {
        List<HistoryStore.Entry> entries = HistoryStore.load(this);
        List<HistoryStore.Entry> visible = new ArrayList<>();
        for (HistoryStore.Entry entry : entries) {
            if (tableFilter != null && !entry.table.equals(tableFilter)) continue;
            visible.add(entry);
        }
        adapter.submit(visible);
        emptyView.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.history_clear_title)
                .setMessage(R.string.history_clear_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    HistoryStore.clear(this);
                    buildFilters();
                    reload();
                })
                .show();
    }

    @Override
    public void onUndo(@NonNull HistoryStore.Entry entry) {
        if (entry.oldValue == null) {
            Toast.makeText(this, getString(R.string.history_undo_failed, entry.key),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.history_undo_title)
                .setMessage(getString(R.string.history_undo_body, entry.key))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.history_undo, (d, w) -> undo(entry))
                .show();
    }

    private void undo(@NonNull HistoryStore.Entry entry) {
        Context appContext = getApplicationContext();
        EXECUTOR.submit(() -> {
            ActionResult result = SettingsUtils.update(
                    appContext, entry.table, entry.key, entry.oldValue);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.successful) {
                    // Record the undo as its own change so the list reflects what just happened.
                    HistoryStore.record(appContext, entry.table, entry.key, entry.newValue, entry.oldValue);
                    Toast.makeText(this, getString(R.string.history_reverted, entry.key),
                            Toast.LENGTH_SHORT).show();
                    reload();
                } else {
                    Toast.makeText(this, getString(R.string.history_undo_failed, entry.key),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
