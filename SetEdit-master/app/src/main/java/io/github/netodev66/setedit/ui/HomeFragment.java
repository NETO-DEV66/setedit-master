package io.github.netodev66.setedit.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.MainActivity;
import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.data.AppPrefs;
import io.github.netodev66.setedit.data.TableMeta;
import io.github.netodev66.setedit.utils.ShellExecutor;

/** The dashboard: privileged-access status, the table list and a few quick actions. */
public class HomeFragment extends Fragment implements TableCardAdapter.Listener,
        QuickActionAdapter.Listener {

    /** Callbacks the host activity implements. */
    public interface Host {
        void openTab(int menuId);

        void openTable(@TableTypeInt int tableType);

        void openRecipes();

        void openConnect();
    }

    private static final ExecutorService COUNT_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-home-counts");
        thread.setDaemon(true);
        return thread;
    });

    @Nullable
    private Host host;
    private TableCardAdapter tableAdapter;
    @NonNull
    private final Map<Integer, Integer> counts = new HashMap<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Host) host = (Host) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView tables = view.findViewById(R.id.home_tables);
        tables.setLayoutManager(new LinearLayoutManager(requireContext()));
        tables.setItemAnimator(null);
        tableAdapter = new TableCardAdapter(this, counts);
        tables.setAdapter(tableAdapter);
        io.github.netodev66.util.UiUtils.playBouncyScreenEntrance(tables);

        RecyclerView quick = view.findViewById(R.id.home_quick);
        quick.setLayoutManager(new LinearLayoutManager(requireContext()));
        quick.setItemAnimator(null);
        quick.setAdapter(new QuickActionAdapter(this, Arrays.asList(
                new QuickActionAdapter.Action(R.drawable.ic_recipes,
                        R.string.home_action_recipes, R.string.home_action_recipes_desc, "recipes"),
                new QuickActionAdapter.Action(R.drawable.ic_nav_backup,
                        R.string.home_action_backup, R.string.home_action_backup_desc, "backup"),
                new QuickActionAdapter.Action(R.drawable.ic_nav_favorite,
                        R.string.home_action_favorites, R.string.home_action_favorites_desc, "favorites"),
                new QuickActionAdapter.Action(R.drawable.ic_apps,
                        R.string.home_action_apps, R.string.home_action_apps_desc, "apps"),
                new QuickActionAdapter.Action(R.drawable.ic_update,
                        R.string.update_row, R.string.update_row_desc, "update")
        )));

        view.findViewById(R.id.access_card).setOnClickListener(v -> {
            if (ShellExecutor.getMode() == ShellExecutor.Mode.NONE && host != null) {
                host.openConnect();
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openTab(R.id.nav_settings);
            }
        });

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<TableMeta> metas = new ArrayList<>();
        boolean advanced = AppPrefs.isShowAdvanced(requireContext());
        for (TableMeta meta : TableMeta.all()) {
            if (!meta.advanced || advanced) metas.add(meta);
        }
        if (tableAdapter != null) tableAdapter.submit(metas);

        View view = getView();
        if (view == null) return;
        MaterialTextView title = view.findViewById(R.id.access_title);
        MaterialTextView subtitle = view.findViewById(R.id.access_subtitle);
        ImageView icon = view.findViewById(R.id.access_icon);

        ShellExecutor.Mode mode = ShellExecutor.getMode();
        if (mode == ShellExecutor.Mode.NONE) {
            title.setText(R.string.home_access_none);
            subtitle.setText(R.string.privileged_access_message);
            icon.setImageResource(R.drawable.ic_lock);
        } else {
            title.setText(R.string.home_access_active);
            switch (mode) {
                case ROOT:
                    subtitle.setText(R.string.home_access_root);
                    break;
                case SHIZUKU:
                    subtitle.setText(R.string.home_access_shizuku);
                    break;
                default:
                    subtitle.setText(R.string.home_access_adb);
                    break;
            }
            icon.setImageResource(R.drawable.ic_shield);
        }

        loadCounts();
    }

    /** Reads the row count of each settings table off the main thread so the cards can show it. */
    private void loadCounts() {
        Context appContext = requireContext().getApplicationContext();
        COUNT_EXECUTOR.submit(() -> {
            Map<Integer, Integer> result = new HashMap<>();
            int[] tables = {TableTypeInt.TABLE_SYSTEM, TableTypeInt.TABLE_SECURE, TableTypeInt.TABLE_GLOBAL};
            for (int tableType : tables) {
                java.util.List<androidx.core.util.Pair<String, String>> items =
                        io.github.netodev66.setedit.utils.SettingsUtils.readAll(
                                appContext, Tables.settingsTypeOf(tableType));
                result.put(tableType, items == null ? -1 : items.size());
            }
            View view = getView();
            if (view == null) return;
            view.post(() -> {
                counts.clear();
                counts.putAll(result);
                if (tableAdapter != null) tableAdapter.notifyDataSetChanged();
            });
        });
    }

    @Override
    public void onTableClick(@NonNull TableMeta meta) {
        if (host != null) host.openTable(meta.tableType);
    }

    @Override
    public void onActionClick(@NonNull QuickActionAdapter.Action action) {
        if (host == null) return;
        switch (action.id) {
            case "recipes":
                host.openRecipes();
                break;
            case "backup":
                host.openTab(R.id.nav_backup);
                break;
            case "favorites":
                host.openTab(R.id.nav_favorites);
                break;
            case "apps":
                startActivity(io.github.netodev66.setedit.apps.AppsActivity.intent(requireContext()));
                break;
            case "update":
                startActivity(io.github.netodev66.setedit.update.UpdateActivity.intent(requireContext()));
                break;
            default:
                break;
        }
    }
}
