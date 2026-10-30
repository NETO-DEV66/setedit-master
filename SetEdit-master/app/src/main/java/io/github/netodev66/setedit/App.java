package io.github.netodev66.setedit;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.topjohnwu.superuser.Shell;

import io.github.netodev66.setedit.data.AppPrefs;
import io.github.netodev66.setedit.utils.ShellExecutor;

public class App extends Application {
    static {
        // Set settings before the main shell can be created
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ShellExecutor.init(this);
        AppCompatDelegate.setDefaultNightMode(AppPrefs.getThemeMode(this));
        if (AppPrefs.isDynamicColor(this)) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
        registerActivityLifecycleCallbacks(new ActivityAppearanceCallback());
        Shell.getShell();
    }

    public static class ActivityAppearanceCallback implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            io.github.netodev66.util.UiUtils.applyImmersiveMode(activity);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            // Dialogs and the soft keyboard can bring the bars back, so hide them again.
            io.github.netodev66.util.UiUtils.applyImmersiveMode(activity);
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }
    }
}
