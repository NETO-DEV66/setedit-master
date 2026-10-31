package io.github.netodev66.setedit.apps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.AppListLoader;
import io.github.netodev66.setedit.data.InstalledApp;

/** The app list: icon, label, package, size and a System badge. */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    public interface Listener {
        void onAppClick(@NonNull InstalledApp app);
    }

    @NonNull
    private final List<InstalledApp> items = new ArrayList<>();
    @NonNull
    private final Listener listener;

    public AppAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<InstalledApp> apps) {
        items.clear();
        items.addAll(apps);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        io.github.netodev66.util.UiUtils.playBouncyListItem(holder.itemView, position);
        InstalledApp app = items.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.label.setText(app.label);
        holder.packageName.setText(app.packageName);
        String size = AppListLoader.formatSize(app.sizeBytes);
        holder.size.setText(size == null ? "" : size);
        holder.badge.setVisibility(app.system ? View.VISIBLE : View.GONE);
        holder.badge.setText(R.string.apps_system_badge);
        // A disabled or frozen app is dimmed so the state reads at a glance.
        holder.itemView.setAlpha(app.enabled ? 1f : 0.5f);
        io.github.netodev66.util.UiUtils.attachPressBounce(holder.itemView);
        holder.itemView.setOnClickListener(v -> listener.onAppClick(app));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final MaterialTextView label;
        final MaterialTextView packageName;
        final MaterialTextView size;
        final MaterialTextView badge;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
            packageName = itemView.findViewById(R.id.app_package);
            size = itemView.findViewById(R.id.app_size);
            badge = itemView.findViewById(R.id.app_badge);
        }
    }
}
