package io.github.netodev66.setedit.apps;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.AppListLoader;
import io.github.netodev66.setedit.data.InstalledApp;
import io.github.netodev66.setedit.utils.PrivilegedAccess;

/** Lists the apps installed on the device and opens the detail screen for one. */
public class AppsActivity extends AppCompatActivity implements AppAdapter.Listener {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-apps");
        thread.setDaemon(true);
        return thread;
    });

    @NonNull
    private final List<InstalledApp> all = new ArrayList<>();
    private AppAdapter adapter;
    private View emptyView;
    private MaterialTextView emptyText;
    private MaterialTextView countView;
    private LinearProgressIndicator progress;
    @NonNull
    private String query = "";
    /** {@code null} means "all"; otherwise true for system, false for user apps. */
    @Nullable
    private Boolean systemFilter;

    @NonNull
    public static Intent intent(@NonNull Context context) {
        return new Intent(context, AppsActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        MaterialToolbar toolbar = findViewById(R.id.apps_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyView = findViewById(R.id.apps_empty);
        emptyText = findViewById(R.id.apps_empty_text);
        countView = findViewById(R.id.apps_count);
        progress = findViewById(R.id.apps_progress);

        RecyclerView recycler = findViewById(R.id.apps_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(null);
        adapter = new AppAdapter(this);
        recycler.setAdapter(adapter);

        buildFilters();

        TextInputEditText search = findViewById(R.id.apps_search);
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

        load();
    }

    private void buildFilters() {
        ChipGroup group = findViewById(R.id.apps_filters);
        group.removeAllViews();
        addChip(group, getString(R.string.apps_filter_all), null, true);
        addChip(group, getString(R.string.apps_filter_user), false, false);
        addChip(group, getString(R.string.apps_filter_system), true, false);
    }

    private void addChip(@NonNull ChipGroup group, @NonNull String label,
                         @Nullable Boolean system, boolean checked) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setOnClickListener(v -> {
            systemFilter = system;
            applyFilter();
        });
        group.addView(chip);
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        EXECUTOR.submit(() -> {
            List<InstalledApp> apps = AppListLoader.load(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                all.clear();
                all.addAll(apps);
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        List<InstalledApp> visible = new ArrayList<>();
        for (InstalledApp app : all) {
            if (systemFilter != null && app.system != systemFilter) continue;
            if (!query.isEmpty()
                    && !app.label.toLowerCase(Locale.ROOT).contains(query)
                    && !app.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            visible.add(app);
        }
        adapter.submit(visible);
        countView.setText(getString(R.string.apps_count, visible.size()));
        boolean isEmpty = visible.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            emptyText.setText(query.isEmpty()
                    ? getString(R.string.empty_generic)
                    : getString(R.string.apps_empty, query));
        }
    }

    @Override
    public void onAppClick(@NonNull InstalledApp app) {
        startActivity(AppDetailActivity.intent(this, app.packageName));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // A detail action can disable, uninstall or freeze an app; refresh the flags on return.
        if (!all.isEmpty()) load();
        // Warm the privileged route in the background so the first action on a detail screen does
        // not have to wait for a reconnect.
        PrivilegedAccess.ensure(this, (ready, reason) -> {
        });
    }
}
