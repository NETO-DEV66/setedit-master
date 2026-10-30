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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.FavoritesStore;
import io.github.netodev66.setedit.ui.editor.EditorSheet;

/** Searches the system, secure and global tables at once, updating as the user types. */
public class SearchFragment extends Fragment implements SearchAdapter.Listener {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-search");
        thread.setDaemon(true);
        return thread;
    });

    private SearchAdapter adapter;
    private View emptyView;
    private MaterialTextView emptyTitle;
    private MaterialTextView emptyBody;
    private View progress;
    @NonNull
    private String lastQuery = "";
    private int generation = 0;
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private final Runnable pendingSearch = () -> search(lastQuery);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.list_toolbar).setVisibility(View.GONE);
        view.findViewById(R.id.list_fab).setVisibility(View.GONE);

        TextInputEditText input = view.findViewById(R.id.list_search);
        emptyView = view.findViewById(R.id.list_empty);
        emptyTitle = view.findViewById(R.id.list_empty_title);
        emptyBody = view.findViewById(R.id.list_empty_body);
        progress = view.findViewById(R.id.list_progress);

        emptyTitle.setText(R.string.search_prompt_title);
        emptyBody.setText(R.string.search_prompt_body);
        emptyView.setVisibility(View.VISIBLE);

        RecyclerView recycler = view.findViewById(R.id.list_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setItemAnimator(null);
        adapter = new SearchAdapter(this, FavoritesStore.all(requireContext()));
        recycler.setAdapter(adapter);
        io.github.netodev66.util.UiUtils.playBouncyScreenEntrance(recycler);

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                lastQuery = s == null ? "" : s.toString();
                debounce.removeCallbacks(pendingSearch);
                debounce.postDelayed(pendingSearch, 250L);
            }
        });
    }

    private void search(@NonNull String query) {
        lastQuery = query;
        final int token = ++generation;
        if (query.trim().isEmpty()) {
            adapter.submit(Collections.emptyList());
            emptyTitle.setText(R.string.search_prompt_title);
            emptyBody.setText(R.string.search_prompt_body);
            emptyView.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        Context appContext = requireContext().getApplicationContext();
        EXECUTOR.submit(() -> {
            List<Tables.Hit> hits = Tables.searchSettings(appContext, query);
            View view = getView();
            if (view == null || token != generation) return;
            view.post(() -> {
                if (token != generation) return;
                progress.setVisibility(View.GONE);
                adapter.submit(hits);
                boolean isEmpty = hits.isEmpty();
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                if (isEmpty) {
                    emptyTitle.setText(getString(R.string.search_no_results, query));
                    emptyBody.setText("");
                }
            });
        });
    }

    @Override
    public void onResultClick(@NonNull Tables.Hit hit) {
        EditorSheet sheet = EditorSheet.edit(hit.tableType, hit.key, hit.value, true, true, true, true);
        sheet.setListener(new EditorSheet.Listener() {
            @Override
            public void onSave(@NonNull String key, @NonNull String value, boolean isNew) {
                writeAsync(hit.tableType, key, value, isNew);
            }

            @Override
            public void onDelete(@NonNull String key) {
                deleteAsync(hit.tableType, key);
            }

            @Override
            public void onEdited() {
                search(lastQuery);
            }
        });
        sheet.show(getParentFragmentManager(), "editor");
    }

    @Override
    public void onStarClick(@NonNull Tables.Hit hit) {
        FavoritesStore.toggle(requireContext(), Tables.settingsTypeOf(hit.tableType), hit.key);
        adapter.notifyDataSetChanged();
    }

    private void writeAsync(int tableType, @NonNull String key, @NonNull String value, boolean isNew) {
        Context appContext = requireContext().getApplicationContext();
        String settingsType = Tables.settingsTypeOf(tableType);
        EXECUTOR.submit(() -> {
            if (isNew) {
                io.github.netodev66.setedit.utils.SettingsUtils.create(appContext, settingsType, key, value);
            } else {
                io.github.netodev66.setedit.utils.SettingsUtils.update(appContext, settingsType, key, value);
            }
            View view = getView();
            if (view != null) view.post(() -> search(lastQuery));
        });
    }

    private void deleteAsync(int tableType, @NonNull String key) {
        Context appContext = requireContext().getApplicationContext();
        String settingsType = Tables.settingsTypeOf(tableType);
        EXECUTOR.submit(() -> {
            io.github.netodev66.setedit.utils.SettingsUtils.delete(appContext, settingsType, key);
            View view = getView();
            if (view != null) view.post(() -> search(lastQuery));
        });
    }
}
