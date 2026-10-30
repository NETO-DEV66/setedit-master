package io.github.netodev66.setedit.utils;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import io.github.netodev66.setedit.EditorUtils;
import io.github.netodev66.setedit.SettingsType;

/**
 * Reads and writes the {@code system}/{@code secure}/{@code global} tables.
 *
 * <p>Every operation prefers the privileged shell ({@link ShellExecutor}); the
 * {@code SettingsProvider} exempts the shell and root identities from the targetSdk-gated key
 * restrictions that otherwise hide or reject most keys on Android 15+. When no shell is reachable —
 * for example after wireless debugging has been used once but the device is now offline — the
 * {@code ContentResolver} is used instead, which keeps working because the ADB connection grants
 * the app {@code WRITE_SECURE_SETTINGS} before it goes away.
 */
public final class SettingsUtils {
    private static final String SNAPSHOT_PREFIX = "settings_snapshot_";

    /**
     * The last full table seen through the shell, kept so the handful of keys the provider hides
     * from apps keep showing once the connection is gone. Mirrored to disk so the coverage survives
     * a reboot with no network.
     */
    private static final Map<String, Map<String, String>> SNAPSHOT = new ConcurrentHashMap<>();

    private SettingsUtils() {
    }

    /**
     * Reads the whole table, preferring the privileged shell.
     *
     * @return the settings as name/value pairs. When no shell is reachable the fresh
     * {@code ContentResolver} values are returned, extended with any keys the provider hides but a
     * previous shell session recorded. {@code null} is returned only when neither source has
     * anything, so the caller can fall back to its own query.
     */
    @Nullable
    public static List<Pair<String, String>> readAll(@NonNull Context context,
                                                     @SettingsType @NonNull String settingsType) {
        if (ShellExecutor.isAvailable()) {
            ShellExecutor.Result result = ShellExecutor.exec("settings list " + settingsType);
            if (result.isSuccess()) {
                Map<String, String> parsed = new LinkedHashMap<>();
                for (String line : result.stdout) {
                    int separator = line.indexOf('=');
                    if (separator < 0) continue;
                    parsed.put(line.substring(0, separator), line.substring(separator + 1));
                }
                SNAPSHOT.put(settingsType, parsed);
                persistSnapshot(context, settingsType, parsed);
                return toPairs(parsed);
            }
        }
        Map<String, String> cached = SNAPSHOT.get(settingsType);
        if (cached == null) cached = loadSnapshot(context, settingsType);
        Map<String, String> fresh = queryResolver(context, settingsType);
        if (cached == null && fresh == null) return null;
        Map<String, String> merged = new TreeMap<>();
        if (cached != null) merged.putAll(cached);
        // Fresh values win; the cache only fills in the keys the provider hides from apps.
        if (fresh != null) merged.putAll(fresh);
        return toPairs(merged);
    }

    @NonNull
    private static List<Pair<String, String>> toPairs(@NonNull Map<String, String> values) {
        List<Pair<String, String>> items = new ArrayList<>(values.size());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            items.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        return items;
    }

    /** Reads every visible row through the provider, or {@code null} when the query fails. */
    @Nullable
    private static Map<String, String> queryResolver(@NonNull Context context,
                                                     @SettingsType @NonNull String settingsType) {
        try (android.database.Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://settings/" + settingsType),
                new String[]{"name", "value"}, null, null, null)) {
            if (cursor == null) return null;
            Map<String, String> values = new LinkedHashMap<>();
            int nameIndex = cursor.getColumnIndex("name");
            int valueIndex = cursor.getColumnIndex("value");
            while (cursor.moveToNext()) {
                values.put(cursor.getString(nameIndex), cursor.getString(valueIndex));
            }
            return values;
        } catch (Throwable th) {
            return null;
        }
    }

    private static void persistSnapshot(@NonNull Context context, @NonNull String settingsType,
                                        @NonNull Map<String, String> values) {
        try (OutputStream os = new FileOutputStream(snapshotFile(context, settingsType))) {
            os.write(new JSONObject(values).toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private static Map<String, String> loadSnapshot(@NonNull Context context,
                                                    @NonNull String settingsType) {
        File file = snapshotFile(context, settingsType);
        if (!file.exists()) return null;
        try {
            byte[] bytes = new byte[(int) file.length()];
            try (java.io.InputStream is = new java.io.FileInputStream(file)) {
                int read = 0;
                while (read < bytes.length) {
                    int count = is.read(bytes, read, bytes.length - read);
                    if (count < 0) break;
                    read += count;
                }
            }
            JSONObject json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            Map<String, String> values = new LinkedHashMap<>();
            for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
                String key = it.next();
                values.put(key, json.optString(key, null));
            }
            return values;
        } catch (Throwable th) {
            return null;
        }
    }

    @NonNull
    private static File snapshotFile(@NonNull Context context, @NonNull String settingsType) {
        return new File(context.getFilesDir(), SNAPSHOT_PREFIX + settingsType + ".json");
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
            ActionResult shellResult = fromShell(ActionResult.TYPE_DELETE, ShellExecutor.exec(
                    "settings delete " + settingsType + " " + quote(keyName)));
            if (shellResult.successful) return shellResult;
        }
        Boolean isGranted = EditorUtils.checkSettingsPermission(context, settingsType);
        if (isGranted == null) return new ActionResult(ActionResult.TYPE_DELETE, false);
        if (!isGranted && !canWriteDirectly(context, settingsType)) {
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
            ActionResult shellResult = fromShell(actionType, ShellExecutor.exec(
                    "settings put " + settingsType + " " + quote(keyName) + " " + quote(newValue)));
            if (shellResult.successful) return shellResult;
        }
        Boolean isGranted = EditorUtils.checkSettingsPermission(context, settingsType);
        if (isGranted == null) return new ActionResult(actionType, false);
        if (!isGranted && !canWriteDirectly(context, settingsType)) {
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

    /**
     * Whether the app can write this table through {@code ContentResolver} without the per-table
     * permission {@link EditorUtils#checkSettingsPermission} looks for.
     *
     * <p>{@code WRITE_SECURE_SETTINGS} authorises writes to the {@code system} table too, but
     * {@code WRITE_SETTINGS} — the permission the system table normally requires — is managed by a
     * role and cannot be granted with {@code pm grant}. The one-time ADB connection grants {@code
     * WRITE_SECURE_SETTINGS}, so it is accepted here as the offline-capable alternative for {@code
     * system}.
     */
    private static boolean canWriteDirectly(@NonNull Context context, @SettingsType String settingsType) {
        return SettingsType.SYSTEM_SETTINGS.equals(settingsType)
                && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Wraps a shell argument in single quotes so spaces, quotes and symbols survive intact. */
    @NonNull
    private static String quote(@NonNull String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
