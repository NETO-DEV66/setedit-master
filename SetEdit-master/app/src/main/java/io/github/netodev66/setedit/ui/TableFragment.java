package io.github.netodev66.setedit.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.data.AppPrefs;
import io.github.netodev66.setedit.data.FavoritesStore;
import io.github.netodev66.setedit.data.TableMeta;
import io.github.netodev66.setedit.ui.editor.EditorSheet;
import io.github.netodev66.setedit.util.ClipboardUtils;
import io.github.netodev66.setedit.utils.SettingsUtils;

/** Browses a single table: search, edit, favorite, create and delete. */
public class TableFragment extends Fragment implements RowAdapter.Listener {

    private static final String ARG_TABLE = "table";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-table");
        thread.setDaemon(true);
        return thread;
    });

    @TableTypeInt
    private int tableType = TableTypeInt.TABLE_SYSTEM;
    private RowAdapter adapter;
    private View emptyView;
    private MaterialTextView emptyTitle;
    private MaterialTextView emptyBody;
    private ExtendedFloatingActionButton fab;
    @NonNull
    private String query = "";
    @NonNull
    private List<Pair<String, String>> allRows = new ArrayList<>();
    @NonNull
    private SortMode sortMode = SortMode.DEFAULT;
    private boolean loading;
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private final Runnable pendingFilter = this::applyFilter;

    public static TableFragment newInstance(@TableTypeInt int tableType) {
        TableFragment fragment = new TableFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TABLE, tableType);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) tableType = getArguments().getInt(ARG_TABLE, tableType);
        TableMeta meta = TableMeta.of(tableType);

        MaterialToolbar toolbar = view.findViewById(R.id.list_toolbar);
        toolbar.setTitle(meta.titleRes);
        toolbar.setNavigationOnClickListener(v -> requireActivity().finish());
        toolbar.inflateMenu(R.menu.menu_table);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sort) {
                showSortDialog();
                return true;
            }
            return false;
        });

        emptyView = view.findViewById(R.id.list_empty);
        emptyTitle = view.findViewById(R.id.list_empty_title);
        emptyBody = view.findViewById(R.id.list_empty_body);
        fab = view.findViewById(R.id.list_fab);
        fab.setVisibility(Tables.isSettingsTable(tableType) ? View.VISIBLE : View.GONE);
        fab.setOnClickListener(v -> showCreateSheet());

        TextInputEditText input = view.findViewById(R.id.list_search);
        input.setHint(R.string.table_search_hint);
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                query = s == null ? "" : s.toString();
                debounce.removeCallbacks(pendingFilter);
                debounce.postDelayed(pendingFilter, 150L);
            }
        });

        RecyclerView recycler = view.findViewById(R.id.list_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setItemAnimator(null); // rows animate themselves; the default animator fights it
        adapter = new RowAdapter(this, true, Tables.isSettingsTable(tableType),
                FavoritesStore.all(requireContext()));
        recycler.setAdapter(adapter);

        io.github.netodev66.util.UiUtils.playBouncyScreenEntrance(recycler);
        load();
    }

    private void showCreateSheet() {
        if (!Tables.isSettingsTable(tableType)) return;
        EditorSheet sheet = EditorSheet.create(tableType, true, true);
        sheet.setListener(new EditorSheet.Listener() {
            @Override
            public void onSave(@NonNull String key, @NonNull String value, boolean isNew) {
                writeAsync(key, value, isNew);
            }

            @Override
            public void onDelete(@NonNull String key) {
            }

            @Override
            public void onEdited() {
                load();
            }
        });
        sheet.show(getParentFragmentManager(), "editor");
    }

    private void load() {
        if (loading) return;
        loading = true;
        Context appContext = requireContext().getApplicationContext();
        EXECUTOR.submit(() -> {
            List<Pair<String, String>> rows = TableData.load(appContext, tableType);
            View view = getView();
            if (view == null) {
                loading = false;
                return;
            }
            view.post(() -> {
                loading = false;
                allRows = rows;
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        if (adapter == null) return;
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Pair<String, String>> filtered = new ArrayList<>();
        for (Pair<String, String> row : allRows) {
            if (needle.isEmpty()
                    || (row.first != null && row.first.toLowerCase(Locale.ROOT).contains(needle))) {
                filtered.add(row);
            }
        }
        filtered = SortMode.apply(sortMode, filtered);
        adapter.submit(filtered);
        boolean isEmpty = filtered.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            if (needle.isEmpty()) {
                emptyTitle.setText(R.string.empty_generic);
            } else {
                emptyTitle.setText(getString(R.string.table_empty, query));
            }
            emptyBody.setText("");
        }
    }

    /** Lets the user pick how the rows are ordered; the choice is applied immediately. */
    private void showSortDialog() {
        SortMode[] modes = SortMode.values();
        String[] labels = new String[modes.length];
        int checked = 0;
        for (int i = 0; i < modes.length; i++) {
            labels[i] = getString(modes[i].labelRes);
            if (modes[i] == sortMode) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    sortMode = modes[which];
                    applyFilter();
                    dialog.dismiss();
                })
                .show();
    }

    /** Offers to copy the key, the value or the shell command for a long-pressed row. */
    private void showCopyDialog(@NonNull String key, @NonNull String value) {
        CharSequence[] options = {
                getString(R.string.copy_key),
                getString(R.string.copy_value),
                getString(R.string.copy_command)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.copy_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        ClipboardUtils.copy(requireContext(), key, key);
                    } else if (which == 1) {
                        ClipboardUtils.copy(requireContext(), key, value);
                    } else {
                        ClipboardUtils.copy(requireContext(), key,
                                Tables.shellCommand(tableType, key, value));
                    }
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the favorite set in place instead of rebuilding the adapter, so the list keeps
        // its scroll position when returning from the editor.
        if (adapter != null) {
            adapter.setFavorites(FavoritesStore.all(requireContext()));
        }
    }

    @Override
    public void onRowClick(int position, @NonNull String key, @NonNull String value) {
        boolean canEdit = Tables.isSettingsTable(tableType);
        EditorSheet sheet = EditorSheet.edit(tableType, key, value, canEdit, canEdit, canEdit, canEdit);
        sheet.setListener(new EditorSheet.Listener() {
            @Override
            public void onSave(@NonNull String k, @NonNull String v, boolean isNew) {
                writeAsync(k, v, isNew);
            }

            @Override
            public void onDelete(@NonNull String k) {
                deleteAsync(k);
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
        if (!Tables.isSettingsTable(tableType)) return;
        FavoritesStore.toggle(requireContext(), Tables.settingsTypeOf(tableType), key);
        if (adapter != null) adapter.toggleFavorite(position, key);
    }

    @Override
    public void onRowLongClick(int position, @NonNull String key, @NonNull String value) {
        showCopyDialog(key, value);
    }

    private void writeAsync(@NonNull String key, @NonNull String value, boolean isNew) {
        Context appContext = requireContext().getApplicationContext();
        String settingsType = Tables.settingsTypeOf(tableType);
        EXECUTOR.submit(() -> {
            if (isNew) {
                SettingsUtils.create(appContext, settingsType, key, value);
            } else {
                SettingsUtils.update(appContext, settingsType, key, value);
            }
            View view = getView();
            if (view != null) view.post(this::load);
        });
    }

    private void deleteAsync(@NonNull String key) {
        Context appContext = requireContext().getApplicationContext();
        String settingsType = Tables.settingsTypeOf(tableType);
        EXECUTOR.submit(() -> {
            SettingsUtils.delete(appContext, settingsType, key);
            View view = getView();
            if (view != null) view.post(this::load);
        });
    }
}
