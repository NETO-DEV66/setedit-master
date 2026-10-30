package io.github.netodev66.setedit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.Recipe;

/** Renders the list of built-in recipes. */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.Holder> {

    public interface Listener {
        void onApply(@NonNull Recipe recipe);
    }

    private static final String ICON_SPEED = "speed";
    private static final String ICON_TOUCH = "touch";
    private static final String ICON_SCREEN = "screen";
    private static final String ICON_ROTATE = "rotate";
    private static final String ICON_TEXT = "text";
    private static final String ICON_RESTORE = "restore";

    @NonNull
    private final List<Recipe> items = new ArrayList<>();
    @NonNull
    private final Listener listener;

    public RecipeAdapter(@NonNull Listener listener, @NonNull List<Recipe> recipes) {
        this.listener = listener;
        items.addAll(recipes);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_card, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Recipe recipe = items.get(position);
        holder.icon.setImageResource(iconFor(recipe.icon));
        holder.title.setText(recipe.title);
        holder.description.setText(recipe.description);
        holder.apply.setOnClickListener(v -> listener.onApply(recipe));
        holder.itemView.setOnClickListener(v -> listener.onApply(recipe));
    }

    private int iconFor(@NonNull String id) {
        switch (id) {
            case ICON_SPEED:
                return R.drawable.ic_nav_settings;
            case ICON_TOUCH:
                return R.drawable.ic_edit;
            case ICON_SCREEN:
                return R.drawable.ic_table_system;
            case ICON_ROTATE:
                return R.drawable.ic_undo;
            case ICON_TEXT:
                return R.drawable.ic_table_java;
            case ICON_RESTORE:
            default:
                return R.drawable.ic_history;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final MaterialTextView title;
        final MaterialTextView description;
        final MaterialButton apply;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.recipe_icon);
            title = itemView.findViewById(R.id.recipe_title);
            description = itemView.findViewById(R.id.recipe_description);
            apply = itemView.findViewById(R.id.recipe_apply);
        }
    }
}
