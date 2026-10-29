package io.github.netodev66.setedit.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.EditorUtils;
import io.github.netodev66.setedit.SettingsType;

/**
 * Reads and writes the {@code system}/{@code secure}/{@code global} tables.
 *
 * <p>Every operation prefers the privileged shell ({@link ShellExecutor}); the
 * {@code SettingsProvider} exempts the shell and root identities from the targetSdk-gated key
 * restrictions that otherwise hide or reject most keys on Android 15+. {@code ContentResolver} is
 * only used as a fallback when neither root nor Shizuku is available, which keeps the app working
 * on older devices without either.
 */
public final class SettingsUtils {
    private SettingsUtils() {
    }

    /**
     * Reads the whole table through the privileged shell.
     *
     * @return the settings as name/value pairs, or {@code null} when no privileged shell is
     * available so that the caller can fall back to {@code ContentResolver}.
     */
    @Nullable
    public static List<Pair<String, String>> readAll(@SettingsType @NonNull String settingsType) {
        if (!ShellExecutor.isAvailable()) return null;
        ShellExecutor.Result result = ShellExecutor.exec("settings list " + settingsType);
        if (!result.isSuccess()) return null;
        List<Pair<String, String>> items = new ArrayList<>(result.stdout.size());
        for (String line : result.stdout) {
            int separator = line.indexOf('=');
            if (separator < 0) continue;
            items.add(new Pair<>(line.substring(0, separator), line.substring(separator + 1)));
        }
        return items;
    }

    @NonNull
    public static ActionResult create(@NonNull Context context, @SettingsType String settingsType,
                                      @NonNull String keyName, @NonNull String newValue) {
        return updateInternal(context, settingsType, keyName, newValue, ActionResult.TYPE_CREATE);
    }

    @NonNull
    public static ActionResult update(@NonNull Context context, @SettingsType String settingsType,
                                      @NonNull String keyName, @NonNull String newValue) {
        return updateInternal(context, settingsType, keyName, newValue, ActionResult.TYPE_UPDATE);
    }

    @NonNull
    public static ActionResult delete(@NonNull Context context, @SettingsType String settingsType,
                                      @NonNull String keyName) {
        if (ShellExecutor.isAvailable()) {
            return fromShell(ActionResult.TYPE_DELETE, ShellExecutor.exec(
                    "settings delete " + settingsType + " " + quote(keyName)));
        }
        Boolean isGranted = EditorUtils.checkSettingsPermission(context, settingsType);
        if (isGranted == null) return new ActionResult(ActionResult.TYPE_DELETE, false);
        if (!isGranted) {
            EditorUtils.displayGrantPermissionMessage(context);
            return new ActionResult(ActionResult.TYPE_DELETE, false);
        }
        ContentResolver contentResolver = context.getContentResolver();
        try {
            String[] strArr = {keyName};
            contentResolver.delete(Uri.parse("content://settings/" + settingsType), "name = ?", strArr);
            return new ActionResult(ActionResult.TYPE_DELETE, true);
        } catch (Throwable th) {
            th.printStackTrace();
            ActionResult r = new ActionResult(ActionResult.TYPE_DELETE, false);
            r.setLogs(th.getMessage());
            return r;
        }
    }

    @NonNull
    private static ActionResult updateInternal(@NonNull Context context, @SettingsType String settingsType,
                                               @NonNull String keyName, @NonNull String newValue,
                                               @ActionResult.ActionType int actionType) {
        if (ShellExecutor.isAvailable()) {
            return fromShell(actionType, ShellExecutor.exec(
                    "settings put " + settingsType + " " + quote(keyName) + " " + quote(newValue)));
        }
        Boolean isGranted = EditorUtils.checkSettingsPermission(context, settingsType);
        if (isGranted == null) return new ActionResult(actionType, false);
        if (!isGranted) {
            EditorUtils.displayGrantPermissionMessage(context);
            return new ActionResult(actionType, false);
        }
        ContentResolver contentResolver = context.getContentResolver();
        try {
            ContentValues contentValues = new ContentValues(2);
            contentValues.put("name", keyName);
            contentValues.put("value", newValue);
            contentResolver.insert(Uri.parse("content://settings/" + settingsType), contentValues);
            return new ActionResult(actionType, true);
        } catch (Throwable th) {
            th.printStackTrace();
            ActionResult r = new ActionResult(actionType, false);
            r.setLogs(th.getMessage());
            return r;
        }
    }

    @NonNull
    private static ActionResult fromShell(@ActionResult.ActionType int actionType,
                                          @NonNull ShellExecutor.Result result) {
        ActionResult r = new ActionResult(actionType, result.isSuccess());
        if (!result.isSuccess()) r.setLogs(result.errorMessage());
        return r;
    }

    /** Wraps a shell argument in single quotes so spaces, quotes and symbols survive intact. */
    @NonNull
    private static String quote(@NonNull String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
