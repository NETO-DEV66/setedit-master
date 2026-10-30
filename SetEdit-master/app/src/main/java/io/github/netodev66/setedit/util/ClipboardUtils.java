package io.github.netodev66.setedit.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.netodev66.setedit.R;

/** Copies text to the clipboard and shows a short confirmation. */
public final class ClipboardUtils {
    private ClipboardUtils() {
    }

    /** Copies {@code text} and toasts the given confirmation. Returns {@code false} on failure. */
    public static boolean copy(@NonNull Context context, @Nullable CharSequence label,
                               @NonNull String text, int confirmationRes) {
        try {
            ClipboardManager manager =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager == null) return false;
            manager.setPrimaryClip(ClipData.newPlainText(label, text));
            if (confirmationRes != 0) {
                Toast.makeText(context, confirmationRes, Toast.LENGTH_SHORT).show();
            }
            return true;
        } catch (Throwable th) {
            Toast.makeText(context, R.string.copy_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /** Copies {@code text} with the standard "Copied" confirmation. */
    public static boolean copy(@NonNull Context context, @Nullable CharSequence label,
                               @NonNull String text) {
        return copy(context, label, text, R.string.copied);
    }
}
