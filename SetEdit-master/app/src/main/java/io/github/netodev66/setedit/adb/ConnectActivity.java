package io.github.netodev66.setedit.adb;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.AppPrefs;
import io.github.netodev66.setedit.utils.PrivilegedAccess;
import io.github.netodev66.setedit.utils.ShellExecutor;

/**
 * One-time screen that pairs the app with this device's own ADB over wireless debugging.
 *
 * <p>The user turns on wireless debugging, opens the "Pair device with pairing code" dialog, types
 * the code shown there, and taps Connect. SetEdit discovers the pairing service over mDNS, pairs,
 * then connects; from then on the key is remembered and this screen never asks again.
 *
 * <p>The code can also be typed straight from a notification's inline-reply field, so the user does
 * not have to switch back and forth between the wireless-debugging dialog and this screen.
 */
public class ConnectActivity extends AppCompatActivity {

    /** Request code for the Shizuku permission prompt, so the answer can be matched. */
    private static final int REQUEST_SHIZUKU = 4702;

    @Nullable
    private LinearProgressIndicator progress;
    @Nullable
    private View statusView;
    @Nullable
    private EditText pairingCode;
    @Nullable
    private MaterialButton connectButton;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> AdbPairingNotification.show(this));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        statusView = findViewById(R.id.status);
        pairingCode = findViewById(R.id.pairing_code);
        connectButton = findViewById(R.id.connect_button);
        MaterialButton skipButton = findViewById(R.id.skip_button);
        MaterialButton notifyButton = findViewById(R.id.notify_button);

        connectButton.setOnClickListener(v -> startPairing());
        skipButton.setOnClickListener(v -> finish());
        if (notifyButton != null) {
            notifyButton.setOnClickListener(v -> showPairingNotification());
        }
        animateIn(findViewById(R.id.pairing_code_layout));

        // Try a silent connection first: if this device already trusts the app (paired before, or
        // authorised another way) it connects without asking for anything. The pairing notification
        // is posted only when that silent attempt shows a code is actually needed.
        setStatus(R.string.connect_connecting);
        setBusy(true);
        AdbConnector.runAsync(() -> {
            // Root or an already-approved Shizuku service means nothing else is needed.
            if (ShellExecutor.isAvailable()) {
                runOnUiThread(() -> onConnectResult(AdbConnector.State.CONNECTED, false));
                return;
            }
            // A Shizuku service that has not been approved yet: ask, and use it if granted.
            if (ShellExecutor.isShizukuBinderAvailable()) {
                // requestPermission shows a system dialog, so it must run on the main thread.
                runOnUiThread(() -> {
                    try {
                        rikka.shizuku.Shizuku.requestPermission(REQUEST_SHIZUKU);
                    } catch (Throwable ignored) {
                    }
                });
                for (int i = 0; i < 40 && !ShellExecutor.isShizukuReady(); i++) {
                    try {
                        Thread.sleep(250L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (ShellExecutor.isShizukuReady()) {
                    ShellExecutor.grantPrivilegedPermissions(this);
                    runOnUiThread(() -> onConnectResult(AdbConnector.State.CONNECTED, false));
                    return;
                }
            }
            AdbConnector.State state = AdbConnector.connect(this);
            runOnUiThread(() -> onConnectResult(state, false));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The code may have been entered from the notification while this screen was in the
        // background; reflect that so the screen does not keep asking.
        if (AdbConnector.isConnected()) {
            AdbPairingNotification.cancel(this);
            setResult(RESULT_OK);
            finish();
        }
    }

    /** Posts the pairing notification, asking for the notification permission first on Android 13+. */
    private void showPairingNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        AdbPairingNotification.show(this);
    }

    private void startPairing() {
        String code = pairingCode == null ? "" : String.valueOf(pairingCode.getText()).trim();
        if (code.length() != 6) {
            if (pairingCode != null) pairingCode.setError(getString(R.string.connect_pairing_failed));
            return;
        }
        setStatus(R.string.connect_searching);
        setBusy(true);
        AdbConnector.runAsync(() -> {
            AdbConnector.State state = AdbConnector.pairAndConnect(this, code);
            runOnUiThread(() -> onConnectResult(state, true));
        });
    }

    private void onConnectResult(@NonNull AdbConnector.State state, boolean wasPairing) {
        setBusy(false);
        if (state == AdbConnector.State.CONNECTED) {
            AdbPairingNotification.cancel(this);
            setStatus(R.string.connect_connected);
            connectButton.setText(R.string.connected_status);
            connectButton.setEnabled(false);
            // Everything useful is granted in this one pass, so no later action has to ask again.
            grantEverything();
            getWindow().getDecorView().postDelayed(() -> {
                setResult(RESULT_OK);
                finish();
            }, 900L);
            return;
        }
        connectButton.setText(R.string.connect_retry);
        if (wasPairing) {
            setStatus(R.string.connect_pairing_failed);
            showPairingNotification();
        } else if (state == AdbConnector.State.NEEDS_PAIRING) {
            // The device knows wireless debugging but does not trust this app yet: this is the
            // moment a pairing code is required, so offer the code entry in a notification as well
            // as in this screen.
            showPairingCode();
            setStatus(R.string.privileged_access_message);
            showPairingNotification();
        } else {
            showPairingCode();
            setStatus(R.string.connect_wireless_debugging_off);
        }
    }

    /**
     * Grants the whole privileged permission set once the connection is up.
     *
     * <p>Shizuku is asked for its permission first, when a service is running, so a device that has
     * Shizuku does not have to keep the wireless-debugging connection at all. The grant itself runs
     * shell commands, so it goes to a background thread.
     */
    private void grantEverything() {
        AppPrefs.setShizukuDenied(this, false);
        if (PrivilegedAccess.isShizukuAvailable() && !ShellExecutor.isShizukuReady()) {
            rikka.shizuku.Shizuku.requestPermission(REQUEST_SHIZUKU);
        }
        Context appContext = getApplicationContext();
        AdbConnector.runAsync(() -> ShellExecutor.grantPrivilegedPermissions(appContext));
    }

    private void setBusy(boolean busy) {
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (connectButton != null) connectButton.setEnabled(!busy);
    }

    private void setStatus(int stringRes) {
        if (statusView instanceof android.widget.TextView) {
            ((android.widget.TextView) statusView).setText(stringRes);
            statusView.setVisibility(View.VISIBLE);
        }
    }

    private void showPairingCode() {
        View layout = findViewById(R.id.pairing_code_layout);
        if (layout != null) layout.setVisibility(View.VISIBLE);
    }

    private void animateIn(@Nullable View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360L)
                .setInterpolator(AnimationUtils.loadInterpolator(this, R.interpolator.m3_spring_bouncy))
                .start();
    }
}
