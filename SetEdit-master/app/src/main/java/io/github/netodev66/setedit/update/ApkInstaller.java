package io.github.netodev66.setedit.update;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;

/** Hands a downloaded APK to the system package installer. */
public final class ApkInstaller {

    private static final String AUTHORITY_SUFFIX = ".fileprovider";

    private ApkInstaller() {
    }

    /** The content:// URI the installer needs; a plain file:// URI is rejected on Android 7+. */
    @NonNull
    public static Uri uriFor(@NonNull Context context, @NonNull File apk) {
        String authority = context.getPackageName() + AUTHORITY_SUFFIX;
        return FileProvider.getUriForFile(context, authority, apk);
    }

    /**
     * Starts the package installer for {@code apk}.
     *
     * <p>Returns {@code false} when the device does not allow installing unknown apps yet; the
     * caller should send the user to {@link #settingsIntent(Context)} first.
     */
    public static boolean install(@NonNull Context context, @NonNull File apk) {
        if (!UpdateChecker.canInstall(context)) return false;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uriFor(context, apk), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** The per-app "install unknown apps" screen (Android 8+) or the global one below that. */
    @Nullable
    public static Intent settingsIntent(@NonNull Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent.resolveActivity(context.getPackageManager()) != null ? intent : null;
    }
}
