package io.github.netodev66.setedit.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        // Android 15+ forbids a BOOT_COMPLETED receiver from launching a specialUse foreground
        // service, which aborted the process at boot. The boot actions are a short one-shot job,
        // so run them straight from the receiver with goAsync() instead of starting a service.
        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                BootUtils.runBootActions(appContext);
            } finally {
                pendingResult.finish();
            }
        }, "setedit-boot-actions").start();
    }
}
