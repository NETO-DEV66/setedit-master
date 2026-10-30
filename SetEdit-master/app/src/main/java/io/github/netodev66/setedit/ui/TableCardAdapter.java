package io.github.netodev66.setedit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.TableMeta;

/** The dashboard's list of tables. */
public class TableCardAdapter extends RecyclerView.Adapter<TableCardAdapter.Holder> {

    public interface Listener {
        void onTableClick(@NonNull TableMeta meta);
    }

    @NonNull
    private final List<TableMeta> items = new ArrayList<>();
    @NonNull
    private final Listener listener;
    @NonNull
    private final Map<Integer, Integer> counts;

    public TableCardAdapter(@NonNull Listener listener, @NonNull Map<Integer, Integer> counts) {
        this.listener = listener;
        this.counts = counts;
    }

    public void submit(@NonNull List<TableMeta> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table_card, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        io.github.netodev66.util.UiUtils.playBouncyListItem(holder.itemView, position);
        TableMeta meta = items.get(position);
        holder.icon.setImageResource(meta.iconRes);
        holder.title.setText(meta.titleRes);
        holder.description.setText(meta.descriptionRes);
        Integer count = counts.get(meta.tableType);
        if (count != null && count >= 0) {
            holder.count.setVisibility(View.VISIBLE);
            holder.count.setText(String.valueOf(count));
        } else {
            holder.count.setVisibility(View.GONE);
        }
        io.github.netodev66.util.UiUtils.attachPressBounce(holder.itemView);
        holder.itemView.setOnClickListener(v -> listener.onTableClick(meta));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final MaterialTextView title;
        final MaterialTextView description;
        final MaterialTextView count;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.table_icon);
            title = itemView.findViewById(R.id.table_title);
            description = itemView.findViewById(R.id.table_description);
            count = itemView.findViewById(R.id.table_count);
        }
    }
}
