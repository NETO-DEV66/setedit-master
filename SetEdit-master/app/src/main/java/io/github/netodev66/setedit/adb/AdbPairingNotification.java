package io.github.netodev66.setedit.adb;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;

import io.github.netodev66.setedit.R;

/**
 * Shows the wireless-debugging pairing code entry as a notification.
 *
 * <p>Android's own wireless-debugging dialog shows a pairing code while it is open. Instead of
 * switching back to SetEdit to type it, the user can enter it straight from the notification's
 * inline reply field; the reply is delivered to {@link AdbPairingReceiver}, which runs the pairing.
 */
public final class AdbPairingNotification {
    public static final String CHANNEL_ID = "adb_pairing";
    public static final String ACTION_ENTER_CODE = "io.github.netodev66.setedit.action.ENTER_PAIRING_CODE";
    public static final String EXTRA_CODE = "pairing_code";

    private static final int NOTIFICATION_ID = 4100;
    private static final int REQUEST_REPLY = 0;
    private static final int REQUEST_OPEN = 1;

    private AdbPairingNotification() {
    }

    /** Posts the notification with an inline-reply field for the six-digit code. */
    public static void show(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        createChannel(appContext);
        if (!canNotify(appContext)) return;

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_CODE)
                .setLabel(appContext.getString(R.string.connect_pairing_code_hint))
                .build();

        Intent replyIntent = new Intent(appContext, AdbPairingReceiver.class)
                .setAction(ACTION_ENTER_CODE);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(appContext, REQUEST_REPLY,
                replyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_notification_adb,
                appContext.getString(R.string.pairing_notification_action),
                replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        NotificationCompat.Builder builder = baseBuilder(appContext)
                .setContentTitle(appContext.getString(R.string.pairing_notification_title))
                .setContentText(appContext.getString(R.string.pairing_notification_text))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(appContext.getString(R.string.pairing_notification_text)))
                .addAction(replyAction);
        post(appContext, builder);
    }

    /** Replaces the notification with a progress state once a code has been received. */
    public static void showProgress(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (!canNotify(appContext)) return;
        NotificationCompat.Builder builder = baseBuilder(appContext)
                .setContentTitle(appContext.getString(R.string.connect_pairing))
                .setContentText(appContext.getString(R.string.pairing_notification_wait))
                .setProgress(0, 0, true)
                .setOngoing(true);
        post(appContext, builder);
    }

    /** Replaces the notification with the outcome; a success dismisses itself. */
    public static void showResult(@NonNull Context context, @NonNull AdbConnector.State state) {
        Context appContext = context.getApplicationContext();
        if (!canNotify(appContext)) return;
        if (state == AdbConnector.State.CONNECTED) {
            NotificationCompat.Builder builder = baseBuilder(appContext)
                    .setContentTitle(appContext.getString(R.string.connect_connected))
                    .setOngoing(false)
                    .setAutoCancel(true);
            post(appContext, builder);
            return;
        }
        // Pairing did not go through: offer the reply field again so the code can be retyped.
        show(appContext);
    }

    public static void cancel(@NonNull Context context) {
        NotificationManagerCompat.from(context.getApplicationContext()).cancel(NOTIFICATION_ID);
    }

    private static NotificationCompat.Builder baseBuilder(@NonNull Context appContext) {
        Intent openIntent = new Intent(appContext, ConnectActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(appContext, REQUEST_OPEN, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_adb)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false);
    }

    private static void post(@NonNull Context appContext, @NonNull NotificationCompat.Builder builder) {
        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build());
    }

    /** Creates the high-importance channel that carries the reply field. */
    public static void createChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.pairing_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.pairing_channel_description));
        manager.createNotificationChannel(channel);
    }

    private static boolean canNotify(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
