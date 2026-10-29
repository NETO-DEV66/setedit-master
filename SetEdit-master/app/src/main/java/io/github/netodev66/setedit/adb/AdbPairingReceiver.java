package io.github.netodev66.setedit.adb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.RemoteInput;

/**
 * Receives the pairing code typed into the notification's inline-reply field and runs the pairing.
 *
 * <p>Pairing blocks on mDNS discovery and a socket handshake, so it runs on {@link AdbConnector}'s
 * background thread; the outcome is shown back in the same notification.
 */
public class AdbPairingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null || !AdbPairingNotification.ACTION_ENTER_CODE.equals(intent.getAction())) {
            return;
        }
        Bundle results = RemoteInput.getResultsFromIntent(intent);
        String code = results == null ? null : String.valueOf(results.getCharSequence(
                AdbPairingNotification.EXTRA_CODE)).trim();
        if (code == null || !code.matches("\\d{6}")) {
            AdbPairingNotification.show(context);
            return;
        }
        Context appContext = context.getApplicationContext();
        AdbPairingNotification.showProgress(appContext);
        AdbConnector.runAsync(() -> {
            AdbConnector.State state = AdbConnector.pairAndConnect(appContext, code);
            AdbPairingNotification.showResult(appContext, state);
        });
    }
}
