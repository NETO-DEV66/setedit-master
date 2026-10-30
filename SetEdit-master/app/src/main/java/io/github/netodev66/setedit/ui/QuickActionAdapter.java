package io.github.netodev66.setedit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;

/** The dashboard's quick-action rows (recipes, backup, favorites). */
public class QuickActionAdapter extends RecyclerView.Adapter<QuickActionAdapter.Holder> {

    public interface Listener {
        void onActionClick(@NonNull Action action);
    }

    public static final class Action {
        @DrawableRes
        public final int icon;
        @StringRes
        public final int title;
        @StringRes
        public final int description;
        @NonNull
        public final String id;

        public Action(@DrawableRes int icon, @StringRes int title, @StringRes int description,
                      @NonNull String id) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.id = id;
        }
    }

    @NonNull
    private final List<Action> items = new ArrayList<>();
    @NonNull
    private final Listener listener;

    public QuickActionAdapter(@NonNull Listener listener, @NonNull List<Action> actions) {
        this.listener = listener;
        items.addAll(actions);
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
        Action action = items.get(position);
        holder.icon.setImageResource(action.icon);
        holder.title.setText(action.title);
        holder.description.setText(action.description);
        holder.itemView.setOnClickListener(v -> listener.onActionClick(action));
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
