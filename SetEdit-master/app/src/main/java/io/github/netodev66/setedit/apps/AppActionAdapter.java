package io.github.netodev66.setedit.apps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;

/**
 * The app detail's action rows. Unlike {@link io.github.netodev66.setedit.ui.QuickActionAdapter}
 * the title and description are already resolved strings, because the labels change with the app's
 * state (Enable vs Disable, Freeze vs Unfreeze).
 */
public class AppActionAdapter extends RecyclerView.Adapter<AppActionAdapter.Holder> {

    public interface Listener {
        void onAction(@NonNull Action action);
    }

    public static final class Action {
        @DrawableRes
        public final int icon;
        @NonNull
        public final String title;
        @NonNull
        public final String description;
        @NonNull
        public final String id;
        public final boolean dangerous;

        public Action(@DrawableRes int icon, @NonNull String title, @NonNull String description,
                      @NonNull String id, boolean dangerous) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.id = id;
            this.dangerous = dangerous;
        }
    }

    @NonNull
    private final List<Action> items = new ArrayList<>();
    @NonNull
    private final Listener listener;

    public AppActionAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<Action> actions) {
        items.clear();
        items.addAll(actions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_action, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        io.github.netodev66.util.UiUtils.playBouncyListItem(holder.itemView, position);
        Action action = items.get(position);
        holder.icon.setImageResource(action.icon);
        holder.title.setText(action.title);
        holder.description.setText(action.description);
        io.github.netodev66.util.UiUtils.attachPressBounce(holder.itemView);
        holder.itemView.setOnClickListener(v -> listener.onAction(action));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final MaterialTextView title;
        final MaterialTextView description;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.quick_icon);
            title = itemView.findViewById(R.id.quick_title);
            description = itemView.findViewById(R.id.quick_description);
        }
    }
}
