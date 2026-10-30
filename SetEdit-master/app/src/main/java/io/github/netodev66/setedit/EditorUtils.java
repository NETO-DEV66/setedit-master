package io.github.netodev66.setedit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class EditorUtils {
    /**
     * Check whether the permission has been granted
     *
     * @return {@code true} if granted, {@code null} if is being granted and {@code false} otherwise
     */
    @Nullable
    public static Boolean checkSettingsPermission(@NonNull Context context, @SettingsType String settingsType) {
        // WRITE_SECURE_SETTINGS is accepted by the SettingsProvider for every table, including
        // system, and the one-time ADB connection grants it. Short-circuit on it first so the
        // system table never lands on the role-managed WRITE_SETTINGS path below, which cannot be
        // granted over ADB and would otherwise open a system screen and report "not granted" even
        // though the app can already write.
        if (hasSecureWrite(context)) {
            return true;
        }
        String permission = SettingsType.SYSTEM_SETTINGS.equals(settingsType)
                ? Manifest.permission.WRITE_SETTINGS : Manifest.permission.WRITE_SECURE_SETTINGS;
        if (SettingsType.SYSTEM_SETTINGS.equals(settingsType)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                if (Boolean.TRUE.equals(Shell.isAppGrantedRoot())) {
                    Shell.cmd("appops set " + Process.myUid() + " 23 0",
                            "appops set " + BuildConfig.APPLICATION_ID + " 23 0").exec();
                }
                if (!Settings.System.canWrite(context)) {
                    return false;
                }
            }
        } else if (Boolean.TRUE.equals(Shell.isAppGrantedRoot())) {
            Shell.cmd("pm grant " + BuildConfig.APPLICATION_ID + " " + permission).exec();
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Whether a settings write to {@code settingsType} can go ahead, prompting only when it truly
     * cannot.
     *
     * <p>Unlike {@link #checkSettingsPermission}, this never leaves the caller to interpret a
     * {@code null}: when the app cannot write it shows the permission message itself and returns
     * {@code false}.
     */
    public static boolean canWriteSettings(@NonNull Context context, @SettingsType String settingsType) {
        Boolean granted = checkSettingsPermission(context, settingsType);
        if (Boolean.TRUE.equals(granted)) return true;
        if (granted == null) return false;
        displayGrantPermissionMessage(context);
        return false;
    }

    /**
     * Whether the app holds {@code WRITE_SECURE_SETTINGS}.
     *
     * <p>This single permission is what the {@code SettingsProvider} accepts as authorisation to
     * write — and, via the {@code Settings} API, to read — every one of the three tables. It is the
     * permission the one-time ADB connection grants, so checking it first is what lets the app keep
     * working with no network.
     */
    public static boolean hasSecureWrite(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint({"InflateParams", "SetTextI18n"})
    public static void displayGrantPermissionMessage(@NonNull Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_unsupported, null);
        TextView tv = view.findViewById(R.id.txt);
        tv.setText("pm grant " + BuildConfig.APPLICATION_ID + " " + Manifest.permission.WRITE_SECURE_SETTINGS);
        tv.setKeyListener(null);
        tv.setSelectAllOnFocus(true);
        tv.requestFocus();
        new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setNegativeButton(R.string.close, null)
                .show();
    }

    @NonNull
    public static String getJson(@NonNull List<Pair<String, String>> items, @Nullable String settingsType)
            throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if (settingsType != null) {
            jsonObject.put("_settings_type", settingsType);
        }
        for (Pair<String, String> pair : items) {
            jsonObject.put(pair.first, pair.second);
        }
        return jsonObject.toString(4);
    }

    @TableType
    public static String toTableType(@TableTypeInt int tableTypeInt) {
        switch (tableTypeInt) {
            case TableTypeInt.TABLE_SYSTEM:
                return TableType.TABLE_SYSTEM;
            case TableTypeInt.TABLE_SECURE:
                return TableType.TABLE_SECURE;
            case TableTypeInt.TABLE_GLOBAL:
                return TableType.TABLE_GLOBAL;
            case TableTypeInt.TABLE_PROPERTIES:
                return TableType.TABLE_PROPERTIES;
            case TableTypeInt.TABLE_JAVA:
                return TableType.TABLE_JAVA;
            case TableTypeInt.TABLE_ENV:
                return TableType.TABLE_ENV;
            case TableTypeInt.TABLE_BOOT:
                return TableType.TABLE_BOOT;
            case TableTypeInt.TABLE_SHORTCUTS:
                return TableType.TABLE_SHORTCUTS;
            default:
                throw new IllegalArgumentException("Invalid table type: " + tableTypeInt);
        }
    }
}
