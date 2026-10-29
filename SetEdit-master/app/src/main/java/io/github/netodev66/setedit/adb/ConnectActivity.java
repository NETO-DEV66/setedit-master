package io.github.netodev66.setedit.adb;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import io.github.netodev66.setedit.R;

/**
 * One-time screen that pairs the app with this device's own ADB over wireless debugging.
 *
 * <p>The user turns on wireless debugging, opens the "Pair device with pairing code" dialog, types
 * the code shown there, and taps Connect. SetEdit discovers the pairing service over mDNS, pairs,
 * then connects; from then on the key is remembered and this screen never asks again.
 */
public class ConnectActivity extends AppCompatActivity {
    @Nullable
    private LinearProgressIndicator progress;
    @Nullable
    private View statusView;
    @Nullable
    private EditText pairingCode;
    @Nullable
    private MaterialButton connectButton;

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

        connectButton.setOnClickListener(v -> startPairing());
        skipButton.setOnClickListener(v -> finish());
        animateIn(findViewById(R.id.pairing_code_layout));

        // Try a silent connection first: if this device already trusts the app (paired before, or
        // authorised another way) it connects without asking for anything.
        setStatus(R.string.connect_connecting);
        setBusy(true);
        AdbConnector.runAsync(() -> {
            AdbConnector.State state = AdbConnector.connect(this);
            runOnUiThread(() -> onConnectResult(state, false));
        });
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
            setStatus(R.string.connect_connected);
            connectButton.setText(R.string.connected_status);
            connectButton.setEnabled(false);
            getWindow().getDecorView().postDelayed(() -> {
                setResult(RESULT_OK);
                finish();
            }, 900L);
            return;
        }
        connectButton.setText(R.string.connect_retry);
        if (wasPairing) {
            setStatus(R.string.connect_pairing_failed);
        } else {
            // The stored key no longer works, so ask for a fresh pairing code.
            showPairingCode();
            setStatus(R.string.connect_wireless_debugging_off);
        }
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
