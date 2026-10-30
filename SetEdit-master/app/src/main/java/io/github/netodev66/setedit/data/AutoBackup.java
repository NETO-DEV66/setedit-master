package io.github.netodev66.setedit.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Writes a timestamped snapshot of every settings table before a sensitive key is changed.
 *
 * <p>The regular {@code HistoryStore} only remembers one previous value per key, which is enough to
 * undo a single edit but not to recover from a value that broke the device. A snapshot keeps a
 * full, restorable copy that {@link BackupManager} can import later. Snapshots are capped so the
 * app's private storage cannot grow without bound.
 */
public final class AutoBackup {
    private static final String DIR = "auto_backups";
    private static final int MAX_SNAPSHOTS = 10;

    private AutoBackup() {
    }

    /** Saves a snapshot and returns the file, or {@code null} when the write failed. */
    public static File snapshot(@NonNull Context context, @NonNull String reason) {
        File dir = new File(context.getFilesDir(), DIR);
        if (!dir.exists() && !dir.mkdirs()) return null;
        File file = new File(dir, "snapshot-" + System.currentTimeMillis() + ".json");
        try (OutputStream os = new FileOutputStream(file)) {
            BackupManager.export(context.getApplicationContext(), os);
        } catch (Throwable th) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return null;
        }
        prune(dir);
        return file;
    }

    /** Keeps only the newest {@link #MAX_SNAPSHOTS} files so the directory cannot grow forever. */
    private static void prune(@NonNull File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length <= MAX_SNAPSHOTS) return;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (int i = MAX_SNAPSHOTS; i < files.length; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }
}
