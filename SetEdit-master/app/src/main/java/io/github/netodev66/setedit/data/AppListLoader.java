package io.github.netodev66.setedit.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads the installed packages from {@link PackageManager} for the app list. */
public final class AppListLoader {

    private AppListLoader() {
    }

    /**
     * Returns every package visible to the app, sorted by label.
     *
     * <p>Blocking; call off the main thread. Icons are loaded eagerly because the list shows one per
     * row and deferring them would just move the cost into scroll jank.
     */
    @NonNull
    public static List<InstalledApp> load(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        List<InstalledApp> apps = new ArrayList<>();
        for (PackageInfo info : pm.getInstalledPackages(0)) {
            ApplicationInfo appInfo = info.applicationInfo;
            if (appInfo == null) continue;
            // Skip our own package and packages with no launchable identity at all.
            if (context.getPackageName().equals(info.packageName)) continue;

            CharSequence label = pm.getApplicationLabel(appInfo);
            boolean system = (appInfo.flags
                    & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            boolean enabled = appInfo.enabled;
            boolean installed = isInstalled(pm, info.packageName);
            String installer = installerOf(pm, info);

            apps.add(new InstalledApp(
                    info.packageName,
                    label == null ? info.packageName : label.toString(),
                    system,
                    enabled,
                    installed,
                    installer,
                    info.firstInstallTime,
                    info.lastUpdateTime,
                    sizeOf(appInfo),
                    pm.getApplicationIcon(appInfo)));
        }
        Collections.sort(apps, (a, b) ->
                a.label.compareToIgnoreCase(b.label));
        return apps;
    }

    private static boolean isInstalled(@NonNull PackageManager pm, @NonNull String pkg) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @NonNull
    private static String installerOf(@NonNull PackageManager pm, @NonNull PackageInfo info) {
        try {
            String installer = pm.getInstallerPackageName(info.packageName);
            if (installer == null) return "";
            CharSequence label = null;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(installer, 0));
            } catch (PackageManager.NameNotFoundException ignored) {
                // The installer may not be installed itself; fall back to its package name.
            }
            return label == null ? installer : label.toString();
        } catch (Throwable th) {
            return "";
        }
    }

    /** Sums the base and split APKs so the list can show an approximate size. */
    private static long sizeOf(@NonNull ApplicationInfo info) {
        long total = 0;
        if (info.sourceDir != null) total += new File(info.sourceDir).length();
        if (info.splitSourceDirs != null) {
            for (String split : info.splitSourceDirs) {
                if (split != null) total += new File(split).length();
            }
        }
        return total > 0 ? total : -1;
    }

    /** Human-readable size such as {@code 12.4 MB}, or {@code null} when unknown. */
    @Nullable
    public static String formatSize(long bytes) {
        if (bytes < 0) return null;
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(java.util.Locale.US, "%.0f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb);
        return String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0);
    }
}
