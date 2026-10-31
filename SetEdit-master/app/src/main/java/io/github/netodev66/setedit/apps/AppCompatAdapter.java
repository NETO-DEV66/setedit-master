package io.github.netodev66.setedit.apps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.utils.CompatChange;
import io.github.netodev66.util.UiUtils;

/** One row per compat change the package overrides, with a switch for its value. */
public class AppCompatAdapter extends RecyclerView.Adapter<AppCompatAdapter.Holder> {

    public interface Listener {
        void onToggle(@NonNull CompatChange change, boolean enabled);
    }

    @NonNull
    private final List<CompatChange> items = new ArrayList<>();
    @NonNull
    private final Listener listener;

    public AppCompatAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<CompatChange> changes) {
        items.clear();
        items.addAll(changes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_compat, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        UiUtils.playBouncyListItem(holder.itemView, position);
        CompatChange change = items.get(position);
        holder.name.setText(change.name);
        holder.id.setText(holder.itemView.getContext()
                .getString(R.string.app_compat_change_id, change.id));
        // Detach first so recycling a row cannot fire the listener with the previous value.
        holder.toggle.setOnCheckedChangeListener(null);
        holder.toggle.setChecked(change.enabled);
        holder.toggle.setOnCheckedChangeListener((button, checked) ->
                listener.onToggle(change, checked));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialTextView name;
        final MaterialTextView id;
        final MaterialSwitch toggle;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.app_compat_name);
            id = itemView.findViewById(R.id.app_compat_id);
            toggle = itemView.findViewById(R.id.app_compat_switch);
        }
    }
}
