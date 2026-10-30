package io.github.netodev66.setedit.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import io.github.netodev66.setedit.MainActivity;
import io.github.netodev66.setedit.R;

/**
 * A home-screen widget listing the user's favorite settings keys.
 *
 * <p>The list is filled by {@link FavoriteWidgetService}; this provider only wires the widget's
 * click to the app and asks for a refresh when the favorites change or the app starts.
 */
public class SetEditWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager manager,
                         @NonNull int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            manager.updateAppWidget(appWidgetId, buildViews(context));
        }
    }

    /** Rebuilds every placed widget, e.g. after the favorites set changes. */
    public static void refreshAll(@NonNull Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, SetEditWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;
        for (int id : ids) {
            manager.updateAppWidget(id, buildViews(context));
        }
    }

    @NonNull
    private static RemoteViews buildViews(@NonNull Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_favorites);
        views.setRemoteAdapter(R.id.widget_list, new Intent(context, FavoriteWidgetService.class));
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        views.setOnClickPendingIntent(R.id.widget_empty, pending);
        return views;
    }
}
