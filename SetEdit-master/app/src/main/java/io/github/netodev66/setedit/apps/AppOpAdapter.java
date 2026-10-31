package io.github.netodev66.setedit.apps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.util.UiUtils;

/** One row per AppOps operation: its name, the platform default and the mode currently in force. */
public class AppOpAdapter extends RecyclerView.Adapter<AppOpAdapter.Holder> {

    public interface Listener {
        void onOpClick(@NonNull AppOpRow row);
    }

    /** A rendered op row: the name, the mode in force and whether it differs from the default. */
    public static final class AppOpRow {
        @NonNull
        public final String name;
        @NonNull
        public final String mode;
        @NonNull
        public final String defaultMode;

        public AppOpRow(@NonNull String name, @NonNull String mode, @NonNull String defaultMode) {
            this.name = name;
            this.mode = mode;
            this.defaultMode = defaultMode;
        }

        /** True when the mode is not the platform default, so it is worth calling out. */
        public boolean isChanged() {
            return !mode.equals(defaultMode);
        }
    }

    @NonNull
    private final List<AppOpRow> items = new ArrayList<>();
    @NonNull
    private final Listener listener;

    public AppOpAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<AppOpRow> rows) {
        items.clear();
        items.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_op, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        UiUtils.playBouncyListItem(holder.itemView, position);
        AppOpRow row = items.get(position);
        holder.name.setText(row.name);
        holder.mode.setText(row.mode);
        holder.defaultMode.setText(holder.itemView.getContext()
                .getString(R.string.app_ops_default_line, row.defaultMode));
        holder.itemView.setAlpha(row.isChanged() ? 1f : 0.72f);
        UiUtils.attachPressBounce(holder.itemView);
        holder.itemView.setOnClickListener(v -> listener.onOpClick(row));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialTextView name;
        final MaterialTextView mode;
        final MaterialTextView defaultMode;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.app_op_name);
            mode = itemView.findViewById(R.id.app_op_mode);
            defaultMode = itemView.findViewById(R.id.app_op_default);
        }
    }
}
