package io.github.netodev66.setedit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.HistoryStore;

/** Lists recorded edits, newest first, each with an Undo action. */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    public interface Listener {
        void onUndo(@NonNull HistoryStore.Entry entry);
    }

    @NonNull
    private final List<HistoryStore.Entry> entries = new ArrayList<>();
    @NonNull
    private final Listener listener;

    public HistoryAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<HistoryStore.Entry> items) {
        entries.clear();
        entries.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        io.github.netodev66.util.UiUtils.playBouncyListItem(holder.itemView, position);
        HistoryStore.Entry entry = entries.get(position);
        holder.key.setText(entry.key);
        holder.values.setText(holder.itemView.getContext().getString(
                R.string.history_old_value, display(entry.oldValue), display(entry.newValue)));
        holder.table.setText(entry.table);
        holder.time.setText(formatTime(entry.timestamp));
        holder.undo.setEnabled(entry.oldValue != null);
        holder.undo.setOnClickListener(v -> listener.onUndo(entry));
        holder.itemView.setOnClickListener(v -> listener.onUndo(entry));
    }

    @NonNull
    private String display(String value) {
        return value == null ? "" : value;
    }

    @NonNull
    private String formatTime(long timestamp) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return format.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialTextView key;
        final MaterialTextView values;
        final MaterialTextView table;
        final MaterialTextView time;
        final MaterialButton undo;

        Holder(@NonNull View itemView) {
            super(itemView);
            key = itemView.findViewById(R.id.history_key);
            values = itemView.findViewById(R.id.history_values);
            table = itemView.findViewById(R.id.history_table);
            time = itemView.findViewById(R.id.history_time);
            undo = itemView.findViewById(R.id.history_undo);
        }
    }
}
