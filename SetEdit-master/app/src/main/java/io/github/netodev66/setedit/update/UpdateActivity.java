package io.github.netodev66.setedit.update;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.Locale;

import io.github.netodev66.setedit.R;
import io.github.netodev66.util.UiUtils;

/**
 * The in-app updater: checks GitHub for a newer release, downloads its APK and hands it to the
 * package installer.
 */
public class UpdateActivity extends AppCompatActivity {

    public static Intent intent(@NonNull Context context) {
        return new Intent(context, UpdateActivity.class);
    }

    private CircularProgressIndicator spinner;
    private ImageView statusIcon;
    private MaterialTextView statusTitle;
    private MaterialTextView statusBody;
    private LinearProgressIndicator progress;
    private MaterialTextView progressLabel;
    private MaterialTextView notesTitle;
    private View notesCard;
    private MaterialTextView notes;
    private MaterialButton checkButton;
    private MaterialButton actionButton;

    @Nullable
    private UpdateInfo info;
    @Nullable
    private File downloaded;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        MaterialToolbar toolbar = findViewById(R.id.update_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        spinner = findViewById(R.id.update_spinner);
        statusIcon = findViewById(R.id.update_status_icon);
        statusTitle = findViewById(R.id.update_status_title);
        statusBody = findViewById(R.id.update_status_body);
        progress = findViewById(R.id.update_progress);
        progressLabel = findViewById(R.id.update_progress_label);
        notesTitle = findViewById(R.id.update_notes_title);
        notesCard = findViewById(R.id.update_notes_card);
        notes = findViewById(R.id.update_notes);
        checkButton = findViewById(R.id.update_check);
        actionButton = findViewById(R.id.update_action);

        MaterialTextView current = findViewById(R.id.update_current);
        current.setText(getString(R.string.update_current, UpdateManager.currentVersion()));

        checkButton.setOnClickListener(v -> startCheck());
        actionButton.setOnClickListener(v -> onAction());

        UiUtils.playBouncyEntrance(findViewById(R.id.update_hero));
        UiUtils.playBouncyEntrance(findViewById(R.id.update_status_card));

        // Reuse the last result if a check already ran in this process, so returning to the
        // screen does not hit the network again.
        UpdateInfo cached = UpdateManager.cached();
        if (cached != null) {
            bind(cached);
        } else {
            startCheck();
        }
    }

    private void startCheck() {
        if (downloaded != null) {
            UpdateManager.discard(downloaded);
            downloaded = null;
        }
        setChecking(true);
        UpdateManager.check(this, this::bind);
    }

    private void bind(@NonNull UpdateInfo result) {
        info = result;
        setChecking(false);
        hideProgress();

        switch (result.status) {
            case AVAILABLE:
                statusIcon.setImageResource(R.drawable.ic_update);
                statusTitle.setText(getString(R.string.update_available_title, result.version));
                statusBody.setVisibility(View.VISIBLE);
                statusBody.setText(R.string.update_available_body);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(R.string.update_download);
                actionButton.setEnabled(true);
                if (result.notes != null && !result.notes.trim().isEmpty()) {
                    notesTitle.setVisibility(View.VISIBLE);
                    notesCard.setVisibility(View.VISIBLE);
                    notes.setText(result.notes.trim());
                }
                break;
            case UP_TO_DATE:
                statusIcon.setImageResource(R.drawable.ic_check);
                statusTitle.setText(R.string.update_up_to_date);
                statusBody.setVisibility(View.VISIBLE);
                statusBody.setText(getString(R.string.update_up_to_date_body, result.version));
                actionButton.setVisibility(View.GONE);
                notesTitle.setVisibility(View.GONE);
                notesCard.setVisibility(View.GONE);
                break;
            default:
                statusIcon.setImageResource(R.drawable.ic_info);
                statusTitle.setText(R.string.update_failed);
                statusBody.setVisibility(View.VISIBLE);
                statusBody.setText(R.string.update_failed_body);
                actionButton.setVisibility(View.GONE);
                notesTitle.setVisibility(View.GONE);
                notesCard.setVisibility(View.GONE);
                break;
        }
        UiUtils.playBouncyEntrance(statusTitle);
    }

    private void setChecking(boolean checking) {
        spinner.setVisibility(checking ? View.VISIBLE : View.GONE);
        statusIcon.setVisibility(checking ? View.GONE : View.VISIBLE);
        checkButton.setEnabled(!checking);
        if (checking) {
            statusTitle.setText(R.string.update_checking);
            statusBody.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);
            notesTitle.setVisibility(View.GONE);
            notesCard.setVisibility(View.GONE);
        }
    }

    /** The single button under the status: download, then install. */
    private void onAction() {
        if (downloaded != null) {
            install(downloaded);
            return;
        }
        UpdateInfo current = info;
        if (current == null || current.apkUrl == null) {
            // No APK asset on the release; open the release page instead.
            if (current != null && current.releaseUrl != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(current.releaseUrl)));
            }
            return;
        }
        startDownload(current);
    }

    private void startDownload(@NonNull UpdateInfo target) {
        setChecking(true);
        statusTitle.setText(R.string.update_downloading);
        statusBody.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        progressLabel.setVisibility(View.VISIBLE);
        progressLabel.setText(R.string.update_downloading);
        actionButton.setVisibility(View.GONE);

        UpdateManager.download(this, target, new UpdateManager.DownloadCallback() {
            @Override
            public void onStart(int totalBytes) {
                if (totalBytes > 0) {
                    progress.setIndeterminate(false);
                    progress.setMax(totalBytes);
                }
            }

            @Override
            public void onProgress(long bytesRead, int totalBytes) {
                if (totalBytes > 0) {
                    progress.setProgressCompat((int) bytesRead, true);
                    int percent = (int) (bytesRead * 100 / totalBytes);
                    progressLabel.setText(getString(R.string.update_download_percent, percent));
                } else {
                    progressLabel.setText(getString(R.string.update_downloaded_size,
                            humanSize(bytesRead)));
                }
            }

            @Override
            public void onDownloaded(@NonNull File apk) {
                downloaded = apk;
                spinner.setVisibility(View.GONE);
                statusIcon.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.ic_check);
                statusTitle.setText(R.string.update_ready);
                statusBody.setVisibility(View.VISIBLE);
                statusBody.setText(R.string.update_ready_body);
                checkButton.setEnabled(true);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(R.string.update_install);
                actionButton.setEnabled(true);
                hideProgress();
                UiUtils.playBouncyEntrance(statusTitle);
            }

            @Override
            public void onError(@NonNull Exception error) {
                spinner.setVisibility(View.GONE);
                statusIcon.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.ic_info);
                statusTitle.setText(R.string.update_download_failed);
                statusBody.setVisibility(View.VISIBLE);
                statusBody.setText(R.string.update_download_failed_body);
                checkButton.setEnabled(true);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(R.string.update_retry);
                actionButton.setEnabled(true);
                hideProgress();
            }
        });
    }

    private void install(@NonNull File apk) {
        if (!UpdateChecker.canInstall(this)) {
            Toast.makeText(this, R.string.update_allow_install, Toast.LENGTH_LONG).show();
            Intent settings = ApkInstaller.settingsIntent(this);
            if (settings != null) {
                startActivity(settings);
            }
            return;
        }
        if (!ApkInstaller.install(this, apk)) {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideProgress() {
        progress.setVisibility(View.GONE);
        progressLabel.setVisibility(View.GONE);
    }

    @NonNull
    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.0f KB", bytes / 1024f);
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }
}
