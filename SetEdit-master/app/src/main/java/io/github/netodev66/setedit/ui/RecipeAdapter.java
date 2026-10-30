package io.github.netodev66.setedit.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.Recipe;

/** Renders the recipe list, with category grouping, search filtering and custom-recipe actions. */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.Holder> {

    public interface Listener {
        void onApply(@NonNull Recipe recipe);

        /** Long-press on a custom recipe: offer to delete it. */
        default void onLongClick(@NonNull Recipe recipe) {
        }
    }

    private static final String ICON_SPEED = "speed";
    private static final String ICON_SPEED_OFF = "speed_off";
    private static final String ICON_TOUCH = "touch";
    private static final String ICON_TOUCH_OFF = "touch_off";
    private static final String ICON_SCREEN = "screen";
    private static final String ICON_ROTATE = "rotate";
    private static final String ICON_ROTATE_OFF = "rotate_off";
    private static final String ICON_TEXT = "text";
    private static final String ICON_RESTORE = "restore";
    private static final String ICON_CUSTOM = "custom";

    @NonNull
    private final List<Recipe> all = new ArrayList<>();
    @NonNull
    private final List<Recipe> visible = new ArrayList<>();
    @NonNull
    private final Listener listener;
    @NonNull
    private final Context context;
    @NonNull
    private String query = "";
    @Nullable
    private Recipe.Category categoryFilter;

    public RecipeAdapter(@NonNull Context context, @NonNull Listener listener,
                         @NonNull List<Recipe> recipes) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        all.addAll(recipes);
        refilter();
    }

    /** Replaces the whole set and re-applies the current filter. */
    public void submit(@NonNull List<Recipe> recipes) {
        all.clear();
        all.addAll(recipes);
        refilter();
    }

    /** Filters by free text (title, description and keys) and the selected category. */
    public void setFilter(@NonNull String query, @Nullable Recipe.Category category) {
        this.query = query.trim().toLowerCase(Locale.ROOT);
        this.categoryFilter = category;
        refilter();
    }

    /** Whether the current filter matches nothing, so the caller can show its empty state. */
    public boolean isEmpty() {
        return visible.isEmpty();
    }

    private void refilter() {
        visible.clear();
        for (Recipe recipe : all) {
            if (categoryFilter != null && recipe.category != categoryFilter) continue;
            if (!query.isEmpty() && !matches(recipe)) continue;
            visible.add(recipe);
        }
        notifyDataSetChanged();
    }

    private boolean matches(@NonNull Recipe recipe) {
        if (recipe.title(context).toLowerCase(Locale.ROOT).contains(query)) return true;
        if (recipe.description(context).toLowerCase(Locale.ROOT).contains(query)) return true;
        for (Recipe.Change change : recipe.changes) {
            if (change.key.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
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
        io.github.netodev66.util.UiUtils.playBouncyListItem(holder.itemView, position);
        Recipe recipe = visible.get(position);
        Context viewContext = holder.itemView.getContext();
        holder.icon.setImageResource(iconFor(recipe.icon));
        holder.title.setText(recipe.title(viewContext));
        holder.description.setText(recipe.description(viewContext));
        holder.category.setText(recipe.category.labelRes);
        holder.apply.setOnClickListener(v -> listener.onApply(recipe));
        io.github.netodev66.util.UiUtils.attachPressBounce(holder.itemView);
        holder.itemView.setOnClickListener(v -> listener.onApply(recipe));
        holder.itemView.setOnLongClickListener(v -> {
            if (!recipe.isCustom()) return false;
            listener.onLongClick(recipe);
            return true;
        });
    }

    private int iconFor(@NonNull String id) {
        switch (id) {
            case ICON_SPEED:
                return R.drawable.ic_nav_settings;
            case ICON_SPEED_OFF:
                return R.drawable.ic_clear;
            case ICON_TOUCH:
                return R.drawable.ic_edit;
            case ICON_TOUCH_OFF:
                return R.drawable.ic_clear;
            case ICON_SCREEN:
                return R.drawable.ic_table_system;
            case ICON_ROTATE:
                return R.drawable.ic_undo;
            case ICON_ROTATE_OFF:
                return R.drawable.ic_lock;
            case ICON_TEXT:
                return R.drawable.ic_table_java;
            case ICON_CUSTOM:
                return R.drawable.ic_recipes;
            case ICON_RESTORE:
            default:
                return R.drawable.ic_history;
        }
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final MaterialTextView title;
        final MaterialTextView description;
        final MaterialTextView category;
        final MaterialButton apply;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.recipe_icon);
            title = itemView.findViewById(R.id.recipe_title);
            description = itemView.findViewById(R.id.recipe_description);
            category = itemView.findViewById(R.id.recipe_category);
            apply = itemView.findViewById(R.id.recipe_apply);
        }
    }
}
