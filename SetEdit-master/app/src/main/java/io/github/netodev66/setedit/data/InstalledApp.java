package io.github.netodev66.setedit.data;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/** One installed package, as shown in the app list. */
public final class InstalledApp {

    @NonNull
    public final String packageName;
    @NonNull
    public final String label;
    public final boolean system;
    public final boolean enabled;
    /** {@code true} when the app is installed for the current user; {@code false} when only removed. */
    public final boolean installed;
    /** App label of the installer, or empty. */
    @NonNull
    public final String installer;
    public final long firstInstallTime;
    public final long lastUpdateTime;
    /** Best-effort size in bytes from the APK, or {@code -1} when unknown. */
    public final long sizeBytes;
    @NonNull
    public final Drawable icon;

    public InstalledApp(@NonNull String packageName, @NonNull String label, boolean system,
                        boolean enabled, boolean installed, @NonNull String installer,
                        long firstInstallTime, long lastUpdateTime, long sizeBytes,
                        @NonNull Drawable icon) {
        this.packageName = packageName;
        this.label = label;
        this.system = system;
        this.enabled = enabled;
        this.installed = installed;
        this.installer = installer;
        this.firstInstallTime = firstInstallTime;
        this.lastUpdateTime = lastUpdateTime;
        this.sizeBytes = sizeBytes;
        this.icon = icon;
    }
}
