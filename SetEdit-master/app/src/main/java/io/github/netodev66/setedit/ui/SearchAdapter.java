package io.github.netodev66.setedit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.TableMeta;

/** Renders global search results, each labelled with the table it came from. */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.Holder> {

    public interface Listener {
        void onResultClick(@NonNull Tables.Hit hit);

        default void onStarClick(@NonNull Tables.Hit hit) {
        }
    }

    @NonNull
    private final List<Tables.Hit> items = new ArrayList<>();
    @NonNull
    private final Listener listener;
    @NonNull
    private final java.util.Set<String> favorites;

    public SearchAdapter(@NonNull Listener listener, @NonNull java.util.Set<String> favorites) {
        this.listener = listener;
        this.favorites = favorites;
    }

    public void submit(@NonNull List<Tables.Hit> hits) {
        items.clear();
        items.addAll(hits);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        io.github.netodev66.util.UiUtils.playBouncyListItem(holder.itemView, position);
        Tables.Hit hit = items.get(position);
        holder.name.setText(hit.key);
        holder.value.setText(hit.value);
        holder.table.setText(TableMeta.of(hit.tableType).titleRes);
        String tableName = io.github.netodev66.setedit.ui.Tables.settingsTypeOf(hit.tableType);
        boolean favorite = favorites.contains(tableName + "\u0000" + hit.key);
        holder.star.setImageResource(favorite
                ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        holder.star.setOnClickListener(v -> listener.onStarClick(hit));
        io.github.netodev66.util.UiUtils.attachPressBounce(holder.itemView);
        holder.itemView.setOnClickListener(v -> listener.onResultClick(hit));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialTextView name;
        final MaterialTextView value;
        final MaterialTextView table;
        final ImageButton star;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            value = itemView.findViewById(R.id.txtValue);
            table = itemView.findViewById(R.id.txtTable);
            star = itemView.findViewById(R.id.btnStar);
        }
    }
}
