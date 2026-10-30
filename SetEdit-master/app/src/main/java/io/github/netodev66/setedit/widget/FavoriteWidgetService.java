package io.github.netodev66.setedit.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.FavoritesStore;
import io.github.netodev66.setedit.ui.Tables;

/** Supplies the widget's list rows: one favorite per row, with its current value. */
public class FavoriteWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new FavoriteViewsFactory(getApplicationContext(), intent);
    }

    private static final class FavoriteViewsFactory implements RemoteViewsFactory {
        private final Context context;
        private final List<String[]> rows = new ArrayList<>();

        FavoriteViewsFactory(Context context, Intent intent) {
            this.context = context;
        }

        @Override
        public void onCreate() {
            load();
        }

        @Override
        public void onDataSetChanged() {
            load();
        }

        /** Reads the favorite tokens and resolves each key's current value. */
        private void load() {
            rows.clear();
            for (String token : FavoritesStore.all(context)) {
                String[] parts = FavoritesStore.split(token);
                if (parts == null) continue;
                String table = parts[0];
                String key = parts[1];
                String value = Tables.readValue(context, Tables.tableTypeOf(table), key);
                rows.add(new String[]{key, value == null ? "" : value});
            }
        }

        @Override
        public void onDestroy() {
            rows.clear();
        }

        @Override
        public int getCount() {
            return rows.size();
        }

        @NonNull
        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_favorite_row);
            String[] row = rows.get(position);
            views.setTextViewText(R.id.widget_row_key, row[0]);
            views.setTextViewText(R.id.widget_row_value, row[1]);
            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
