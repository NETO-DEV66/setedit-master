package io.github.netodev66.setedit.adapters;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.Native;
import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.utils.ActionResult;
import io.github.netodev66.setedit.utils.AndroidPropertyUtils;

class AndroidPropertiesRecyclerAdapter extends AbsRecyclerAdapter {
    private static final ExecutorService WRITE_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-property-write");
        thread.setDaemon(true);
        return thread;
    });

    private final List<String[]> propertyList = new ArrayList<>();
    private final List<Integer> matchedIndexes = new ArrayList<>();
    private Filter filter;

    public AndroidPropertiesRecyclerAdapter(FragmentActivity context) {
        super(context);
        refresh();
    }

    @Override
    public void refresh() {
        propertyList.clear();
        Native.setPropertyList(propertyList);
        filter();
    }

    @NonNull
    @Override
    public List<Pair<String, String>> getAllItems() {
        List<Pair<String, String>> items = new ArrayList<>(propertyList.size());
        for (String[] pair : propertyList) {
            items.add(new Pair<>(pair[0], pair[1]));
        }
        return items;
    }

    @Override
    public int getListType() {
        return TableTypeInt.TABLE_PROPERTIES;
    }

    @Override
    public boolean canSetOnReboot() {
        return true;
    }

    @Override
    public boolean canCreateShortcut() {
        return true;
    }

    @Override
    public boolean canEdit() {
        return Boolean.TRUE.equals(Shell.isAppGrantedRoot());
    }

    @Override
    public void update(String keyName, String newValue) {
        // Writing a property goes through the privileged shell, which must not run on the UI thread.
        WRITE_EXECUTOR.submit(() -> {
            ActionResult result = AndroidPropertyUtils.update(keyName, newValue);
            context.runOnUiThread(() -> {
                if (result.successful) {
                    refresh();
                } else {
                    setMessage(new SpannableStringBuilder(context.getText(R.string.error_unexpected))
                            .append(" ")
                            .append(result.getLogs()));
                }
            });
        });
    }

    @Override
    public Pair<String, String> getItem(int position) {
        String[] property = propertyList.get(matchedIndexes.get(position));
        return new Pair<>(property[0], property[1]);
    }

    @Override
    public long getItemId(int position) {
        String[] property = propertyList.get(matchedIndexes.get(position));
        return property[0].hashCode();
    }

    @Override
    public int getItemCount() {
        return matchedIndexes.size();
    }

    @Override
    protected Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<Integer> matchedIndexes = new ArrayList<>(propertyList.size());
                    if (TextUtils.isEmpty(constraint)) {
                        for (int i = 0; i < propertyList.size(); ++i) matchedIndexes.add(i);
                    } else {
                        for (int i = 0; i < propertyList.size(); ++i) {
                            String key = propertyList.get(i)[0];
                            if (key.toLowerCase(Locale.ROOT).contains(constraint)) {
                                matchedIndexes.add(i);
                            }
                        }
                    }
                    results.count = matchedIndexes.size();
                    results.values = matchedIndexes;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    //noinspection unchecked
                    AdapterUtils.notifyDataSetChanged(AndroidPropertiesRecyclerAdapter.this, matchedIndexes,
                            (List<Integer>) results.values);
                }
            };
        }
        return filter;
    }
}
