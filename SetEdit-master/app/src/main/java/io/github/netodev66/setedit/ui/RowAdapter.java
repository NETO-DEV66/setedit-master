package io.github.netodev66.setedit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.RiskyKeys;

/** A simple list of key/value rows with an optional type badge, risky-key marker and favorite star. */
public class RowAdapter extends RecyclerView.Adapter<RowAdapter.Holder> {

    public interface Listener {
        void onRowClick(int position, @NonNull String key, @NonNull String value);

        default void onStarClick(int position, @NonNull String key) {
        }

        /** Long-press: offer to copy the key, the value or the shell command. */
        default void onRowLongClick(int position, @NonNull String key, @NonNull String value) {
        }
    }

    @NonNull
    private final List<Pair<String, String>> items = new ArrayList<>();
    @NonNull
    private final Listener listener;
    private final boolean showBadge;
    private final boolean showStar;
    @NonNull
    private final java.util.Set<String> favorites;

    public RowAdapter(@NonNull Listener listener, boolean showBadge, boolean showStar,
                      @NonNull java.util.Set<String> favorites) {
        this.listener = listener;
        this.showBadge = showBadge;
        this.showStar = showStar;
        this.favorites = favorites;
    }

    /** Swaps the favorite set in place, so the list keeps its scroll position on resume. */
    public void setFavorites(@NonNull java.util.Set<String> newFavorites) {
        favorites.clear();
        favorites.addAll(newFavorites);
        notifyDataSetChanged();
    }

    /** Toggles the star for {@code key} without rebinding the whole list. */
    public void toggleFavorite(int position, @NonNull String key) {
        if (favorites.contains(key)) {
            favorites.remove(key);
        } else {
            favorites.add(key);
        }
        if (position >= 0 && position < items.size()) notifyItemChanged(position);
    }

    public void submit(@NonNull List<Pair<String, String>> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    public List<Pair<String, String>> items() {
        return items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_setting, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        io.github.netodev66.util.UiUtils.playBouncyListItem(holder.itemView, position);
        Pair<String, String> item = items.get(position);
        String key = item.first == null ? "" : item.first;
        String value = item.second == null ? "" : item.second;
        holder.name.setText(key);
        holder.value.setText(value);

        if (showBadge) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(badgeText(value));
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        if (showStar) {
            holder.star.setVisibility(View.VISIBLE);
            boolean favorite = favorites.contains(key);
            holder.star.setImageResource(favorite
                    ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            holder.star.setContentDescription(holder.itemView.getContext().getString(
                    favorite ? R.string.remove_favorite : R.string.add_favorite));
            holder.star.setOnClickListener(v -> listener.onStarClick(holder.getBindingAdapterPosition(), key));
        } else {
            holder.star.setVisibility(View.GONE);
        }

        holder.risky.setVisibility(RiskyKeys.isRisky(key) ? View.VISIBLE : View.GONE);

        io.github.netodev66.util.UiUtils.attachPressBounce(holder.itemView);
        holder.itemView.setOnClickListener(v ->
                listener.onRowClick(holder.getBindingAdapterPosition(), key, value));
        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return false;
            listener.onRowLongClick(adapterPosition, key, value);
            return true;
        });
    }

    private CharSequence badgeText(@Nullable String value) {
        ValueType.Type type = ValueType.of(value);
        switch (type) {
            case BOOLEAN:
                return "Bool";
            case NUMBER:
                return "Number";
            default:
                return "Text";
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialTextView name;
        final MaterialTextView value;
        final MaterialTextView badge;
        final ImageButton star;
        final android.widget.ImageView risky;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            value = itemView.findViewById(R.id.txtValue);
            badge = itemView.findViewById(R.id.txtBadge);
            star = itemView.findViewById(R.id.btnStar);
            risky = itemView.findViewById(R.id.imgRisky);
        }
    }
}
