package io.github.netodev66.setedit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.data.FavoritesStore;
import io.github.netodev66.setedit.ui.editor.EditorSheet;

/** Lists the keys the user starred, across every settings table. */
public class FavoritesFragment extends Fragment implements RowAdapter.Listener {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-favorites");
        thread.setDaemon(true);
        return thread;
    });

    private RowAdapter adapter;
    private View emptyView;
    private MaterialTextView emptyTitle;
    private MaterialTextView emptyBody;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.list_toolbar);
        toolbar.setTitle(R.string.favorites_title);
        toolbar.setNavigationIcon(null);
        view.findViewById(R.id.list_search_layout).setVisibility(View.GONE);
        view.findViewById(R.id.list_fab).setVisibility(View.GONE);

        emptyView = view.findViewById(R.id.list_empty);
        emptyTitle = view.findViewById(R.id.list_empty_title);
        emptyBody = view.findViewById(R.id.list_empty_body);

        RecyclerView recycler = view.findViewById(R.id.list_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setItemAnimator(null);
        adapter = new RowAdapter(this, true, true, favorites());
        recycler.setAdapter(adapter);
        io.github.netodev66.util.UiUtils.playBouncyScreenEntrance(recycler);
        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private Set<String> favorites() {
        return FavoritesStore.all(requireContext());
    }

    private void load() {
        android.content.Context appContext = requireContext().getApplicationContext();
        EXECUTOR.submit(() -> {
            List<Pair<String, String>> rows = new ArrayList<>();
            for (String token : FavoritesStore.all(appContext)) {
                String[] parts = FavoritesStore.split(token);
                if (parts == null) continue;
                int tableType = Tables.tableTypeOf(parts[0]);
                String value = Tables.readValue(appContext, tableType, parts[1]);
                rows.add(new Pair<>(parts[1], value == null ? "" : value));
            }
            View view = getView();
            if (view == null) return;
            view.post(() -> {
                if (adapter == null) return;
                adapter.submit(rows);
                boolean isEmpty = rows.isEmpty();
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                if (isEmpty) {
                    emptyTitle.setText(R.string.favorites_empty_title);
                    emptyBody.setText(R.string.favorites_empty_body);
                }
            });
        });
    }

    @Override
    public void onRowClick(int position, @NonNull String key, @NonNull String value) {
        // Resolve which table this favorite belongs to.
        String table = null;
        for (String token : FavoritesStore.all(requireContext())) {
            String[] parts = FavoritesStore.split(token);
            if (parts != null && parts[1].equals(key)) {
                table = parts[0];
                break;
            }
        }
        if (table == null) return;
        int tableType = Tables.tableTypeOf(table);
        EditorSheet sheet = EditorSheet.edit(tableType, key, value, true, true, true, true);
        sheet.setListener(new EditorSheet.Listener() {
            @Override
            public void onSave(@NonNull String k, @NonNull String v, boolean isNew) {
                writeAsync(tableType, k, v, isNew);
            }

            @Override
            public void onDelete(@NonNull String k) {
                deleteAsync(tableType, k);
            }

            @Override
            public void onEdited() {
                load();
            }
        });
        sheet.show(getParentFragmentManager(), "editor");
    }

    @Override
    public void onStarClick(int position, @NonNull String key) {
        for (String token : FavoritesStore.all(requireContext())) {
            String[] parts = FavoritesStore.split(token);
            if (parts != null && parts[1].equals(key)) {
                FavoritesStore.remove(requireContext(), parts[0], parts[1]);
                break;
            }
        }
        load();
    }

    private void writeAsync(@TableTypeInt int tableType, @NonNull String key,
                            @NonNull String value, boolean isNew) {
        android.content.Context appContext = requireContext().getApplicationContext();
        String settingsType = Tables.settingsTypeOf(tableType);
        EXECUTOR.submit(() -> {
            if (isNew) {
                io.github.netodev66.setedit.utils.SettingsUtils.create(appContext, settingsType, key, value);
            } else {
                io.github.netodev66.setedit.utils.SettingsUtils.update(appContext, settingsType, key, value);
            }
            load();
        });
    }

    private void deleteAsync(@TableTypeInt int tableType, @NonNull String key) {
        android.content.Context appContext = requireContext().getApplicationContext();
        String settingsType = Tables.settingsTypeOf(tableType);
        EXECUTOR.submit(() -> {
            io.github.netodev66.setedit.utils.SettingsUtils.delete(appContext, settingsType, key);
            load();
        });
    }
}
