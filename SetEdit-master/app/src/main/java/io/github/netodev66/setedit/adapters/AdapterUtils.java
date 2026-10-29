package io.github.netodev66.setedit.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public final class AdapterUtils {
    public static <T> void notifyDataSetChanged(@NonNull RecyclerView.Adapter<?> adapter, @NonNull List<T> baseList,
                                                @Nullable List<T> newList) {
        if (newList == null) {
            // Filter hands back a null value list when performFiltering threw.
            return;
        }
        int previousCount = baseList.size();
        baseList.clear();
        baseList.addAll(newList);
        int currentCount = baseList.size();
        if (previousCount > currentCount) {
            // Some values removed
            adapter.notifyItemRangeChanged(0, currentCount);
            adapter.notifyItemRangeRemoved(currentCount, previousCount - currentCount);
        } else if (previousCount < currentCount) {
            // Some values added
            adapter.notifyItemRangeChanged(0, previousCount);
            adapter.notifyItemRangeInserted(previousCount, currentCount - previousCount);
        } else {
            // No values added or removed
            adapter.notifyItemRangeChanged(0, previousCount);
        }
    }
}
