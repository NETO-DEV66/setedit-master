package io.github.netodev66.setedit.apps;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.adb.ConnectActivity;
import io.github.netodev66.setedit.data.AppListLoader;
import io.github.netodev66.setedit.utils.AppActions;
import io.github.netodev66.setedit.utils.AppCommands;
import io.github.netodev66.setedit.utils.AppActionResult;
import io.github.netodev66.setedit.utils.PrivilegedAccess;

/** Shows one app and runs the manage actions on it through the privileged shell. */
public class AppDetailActivity extends AppCompatActivity implements AppActionAdapter.Listener {

    private static final String EXTRA_PACKAGE = "package";

    /** AppOps that are genuinely useful to toggle and safe to read back. */
    private static final String[] COMMON_OPS = {
            "WAKE_LOCK",
            "RUN_IN_BACKGROUND",
            "RUN_ANY_IN_BACKGROUND",
            "START_FOREGROUND",
            "POST_NOTIFICATION",
            "VIBRATE",
            "SYSTEM_ALERT_WINDOW",
            "GET_USAGE_STATS",
            "SCHEDULE_EXACT_ALARM",
            "BOOT_COMPLETED",
    };

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "setedit-app-detail");
        thread.setDaemon(true);
        return thread;
    });

    @NonNull
    private String packageName = "";
    private boolean system;
    private boolean enabled = true;

    private AppActionAdapter actionAdapter;
    private AppActionAdapter advancedAdapter;
    private LinearProgressIndicator progress;
    private MaterialTextView statusView;
    private ImageView iconView;
    private MaterialTextView labelView;
    private MaterialTextView packageView;
    private android.widget.LinearLayout infoContainer;

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull String packageName) {
        return new Intent(context, AppDetailActivity.class).putExtra(EXTRA_PACKAGE, packageName);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        packageName = String.valueOf(getIntent().getStringExtra(EXTRA_PACKAGE));
        if (!AppCommands.isValidPackage(packageName)) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.app_detail_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.app_detail_progress);
        statusView = findViewById(R.id.app_detail_status);
        iconView = findViewById(R.id.app_detail_icon);
        labelView = findViewById(R.id.app_detail_label);
        packageView = findViewById(R.id.app_detail_package);
        infoContainer = findViewById(R.id.app_detail_info);

        RecyclerView actions = findViewById(R.id.app_detail_actions);
        actions.setLayoutManager(new LinearLayoutManager(this));
        actions.setItemAnimator(null);
        actionAdapter = new AppActionAdapter(this);
        actions.setAdapter(actionAdapter);

        RecyclerView advanced = findViewById(R.id.app_detail_advanced);
        advanced.setLayoutManager(new LinearLayoutManager(this));
        advanced.setItemAnimator(null);
        advancedAdapter = new AppActionAdapter(this);
        advanced.setAdapter(advancedAdapter);

        bindInfo();
        rebuildActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // onCreate finishes early for a malformed package, but the lifecycle still resumes once.
        if (isFinishing() || packageView == null) return;
        bindInfo();
        rebuildActions();
    }

    /** Fills the header and detail rows straight from {@code PackageManager}. */
    private void bindInfo() {
        PackageManager pm = getPackageManager();
        packageView.setText(packageName);
        infoContainer.removeAllViews();

        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException ignored) {
            // The app may have been uninstalled from another screen; the header stays minimal.
        }

        if (info == null || info.applicationInfo == null) {
            labelView.setText(packageName);
            iconView.setImageResource(R.drawable.ic_apps_off);
            statusView.setVisibility(View.GONE);
            return;
        }

        ApplicationInfo appInfo = info.applicationInfo;
        CharSequence label = pm.getApplicationLabel(appInfo);
        labelView.setText(label == null ? packageName : label);
        Drawable icon;
        try {
            icon = pm.getApplicationIcon(appInfo);
        } catch (Throwable th) {
            icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(this,
                    R.drawable.ic_apps_off);
        }
        iconView.setImageDrawable(icon);

        system = (appInfo.flags
                & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        enabled = appInfo.enabled;
        toolbarTitle(label == null ? packageName : label.toString());
        statusView.setVisibility(View.VISIBLE);
        String kind = getString(system ? R.string.app_kind_system : R.string.app_kind_user);
        String state = enabled ? getString(R.string.app_state_enabled)
                : getString(R.string.apps_disabled_badge);
        statusView.setText(getString(R.string.app_status_line, kind, state));

        addInfoRow(R.string.app_label_package, packageName);
        addInfoRow(R.string.app_label_version,
                info.versionName == null ? getString(R.string.app_none)
                        : info.versionName + " (" + info.versionCode + ")");
        addInfoRow(R.string.app_label_kind,
                getString(system ? R.string.app_kind_system : R.string.app_kind_user));
        long size = sizeOf(appInfo);
        String formatted = AppListLoader.formatSize(size);
        addInfoRow(R.string.app_label_size,
                formatted == null ? getString(R.string.app_none) : formatted);
        String installer = installerName();
        if (!installer.isEmpty()) addInfoRow(R.string.app_label_installer, installer);
        addInfoRow(R.string.app_label_updated, formatDate(info.lastUpdateTime));
    }

    private void toolbarTitle(@NonNull String title) {
        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.app_detail_toolbar);
        toolbar.setTitle(title);
    }

    private void addInfoRow(int labelRes, @NonNull String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_app_info, infoContainer, false);
        ((MaterialTextView) row.findViewById(R.id.info_label)).setText(labelRes);
        ((MaterialTextView) row.findViewById(R.id.info_value)).setText(value);
        infoContainer.addView(row);
    }

    @NonNull
    private String installerName() {
        try {
            String installer = getPackageManager().getInstallerPackageName(packageName);
            if (installer == null) return "";
            try {
                CharSequence label = getPackageManager()
                        .getApplicationLabel(getPackageManager().getApplicationInfo(installer, 0));
                return label == null ? installer : label.toString();
            } catch (PackageManager.NameNotFoundException ignored) {
                return installer;
            }
        } catch (Throwable th) {
            return "";
        }
    }

    @NonNull
    private static String formatDate(long millis) {
        if (millis <= 0) return "—";
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(millis));
    }

    private static long sizeOf(@NonNull ApplicationInfo info) {
        long total = 0;
        if (info.sourceDir != null) total += new java.io.File(info.sourceDir).length();
        if (info.splitSourceDirs != null) {
            for (String split : info.splitSourceDirs) {
                if (split != null) total += new java.io.File(split).length();
            }
        }
        return total > 0 ? total : -1;
    }

    private void rebuildActions() {
        List<AppActionAdapter.Action> actions = new ArrayList<>();
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_play,
                getString(R.string.app_action_open), getString(R.string.app_action_open_desc),
                "open", false));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_stop,
                getString(R.string.app_action_force_stop),
                getString(R.string.app_action_force_stop_desc), "force_stop", false));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_info,
                getString(R.string.app_action_info), getString(R.string.app_action_info_desc),
                "info", false));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_broom,
                getString(R.string.app_action_clear_cache),
                getString(R.string.app_action_clear_cache_desc), "clear_cache", false));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_off,
                getString(R.string.app_action_clear_data),
                getString(R.string.app_action_clear_data_desc), "clear_data", true));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_freeze,
                getString(R.string.app_action_suspend), getString(R.string.app_action_suspend_desc),
                "suspend", false));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_play,
                getString(R.string.app_action_unsuspend),
                getString(R.string.app_action_unsuspend_desc), "unsuspend", false));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_off,
                enabled ? getString(R.string.app_action_disable)
                        : getString(R.string.app_action_enable),
                enabled ? getString(R.string.app_action_disable_desc)
                        : getString(R.string.app_action_enable_desc),
                enabled ? "disable" : "enable", true));
        actions.add(new AppActionAdapter.Action(R.drawable.ic_apps_trash,
                system ? getString(R.string.app_action_uninstall_user)
                        : getString(R.string.app_action_uninstall),
                system ? getString(R.string.app_action_uninstall_user_desc)
                        : getString(R.string.app_action_uninstall_desc),
                "uninstall", true));
        actionAdapter.submit(actions);

        List<AppActionAdapter.Action> advanced = new ArrayList<>();
        advanced.add(new AppActionAdapter.Action(R.drawable.ic_apps_key,
                getString(R.string.app_action_permissions),
                getString(R.string.app_action_permissions_desc), "permissions", false));
        advanced.add(new AppActionAdapter.Action(R.drawable.ic_apps_tune,
                getString(R.string.app_action_appops),
                getString(R.string.app_action_appops_desc), "appops", false));
        advanced.add(new AppActionAdapter.Action(R.drawable.ic_apps_battery,
                getString(R.string.app_action_battery),
                getString(R.string.app_action_battery_desc), "battery", false));
        advanced.add(new AppActionAdapter.Action(R.drawable.ic_apps_layers,
                getString(R.string.app_action_bucket),
                getString(R.string.app_action_bucket_desc), "bucket", false));
        advancedAdapter.submit(advanced);
    }

    @Override
    public void onAction(@NonNull AppActionAdapter.Action action) {
        if (action.id.equals("open") || action.id.equals("info")) {
            openSystem(action.id.equals("info"));
            return;
        }
        if (AppActions.isPrivileged()) {
            dispatch(action);
            return;
        }
        // Access is not up right now. Try to restore it silently — root, a Shizuku grant, or a
        // reconnect to a device that has paired before — and only fall back to the connect screen
        // when this device has never had access at all.
        progress.setVisibility(View.VISIBLE);
        PrivilegedAccess.ensure(this, (ready, reason) -> {
            if (isFinishing() || isDestroyed()) return;
            progress.setVisibility(View.GONE);
            if (ready) {
                dispatch(action);
                return;
            }
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
            if (PrivilegedAccess.needsUserSetup(this)) {
                startActivity(new Intent(this, ConnectActivity.class));
            }
        });
    }

    /** Routes an action once a privileged shell is known to be available. */
    private void dispatch(@NonNull AppActionAdapter.Action action) {
        switch (action.id) {
            case "permissions":
                showPermissions();
                break;
            case "appops":
                showAppOps();
                break;
            case "battery":
                confirmBattery();
                break;
            case "bucket":
                showBuckets();
                break;
            default:
                confirmAndRun(action);
                break;
        }
    }

    /** Opens the app itself, or the system app-details page. */
    private void openSystem(boolean details) {
        try {
            if (details) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + packageName)));
            } else {
                startActivity(getPackageManager().getLaunchIntentForPackage(packageName));
            }
        } catch (ActivityNotFoundException | NullPointerException e) {
            Toast.makeText(this, R.string.app_result_failed_plain, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmAndRun(@NonNull AppActionAdapter.Action action) {
        String message = action.description;
        if (action.dangerous) message += "\n\n" + getString(R.string.app_warning_irreversible);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_confirm_title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> run(action.id, action.title))
                .show();
    }

    private void run(@NonNull String id, @NonNull String label) {
        progress.setVisibility(View.VISIBLE);
        EXECUTOR.submit(() -> {
            AppActionResult result = perform(id);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                if (result.successful) {
                    Toast.makeText(this, getString(R.string.app_result_done, label),
                            Toast.LENGTH_SHORT).show();
                    if (id.equals("uninstall")) {
                        finish();
                        return;
                    }
                    bindInfo();
                    rebuildActions();
                } else {
                    String detail = result.error.isEmpty()
                            ? getString(R.string.app_result_failed_plain)
                            : result.error;
                    Toast.makeText(this, getString(R.string.app_result_failed, detail),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @NonNull
    private AppActionResult perform(@NonNull String id) {
        switch (id) {
            case "force_stop":
                return AppActions.forceStop(packageName);
            case "clear_cache":
                return AppActions.clearCache(packageName);
            case "clear_data":
                return AppActions.clearData(packageName);
            case "enable":
                return AppActions.setEnabled(packageName, true);
            case "disable":
                return AppActions.setEnabled(packageName, false);
            case "suspend":
                return AppActions.setSuspended(packageName, true);
            case "unsuspend":
                return AppActions.setSuspended(packageName, false);
            case "uninstall":
                return AppActions.uninstall(packageName, system);
            default:
                return new AppActionResult(false, java.util.Collections.emptyList(),
                        "unknown action");
        }
    }

    /** Lists the runtime permissions the app declares and lets each be granted or revoked. */
    private void showPermissions() {
        PackageManager pm = getPackageManager();
        List<String> requested = new ArrayList<>();
        try {
            PackageInfo info = pm.getPackageInfo(packageName,
                    PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                requested.addAll(Arrays.asList(info.requestedPermissions));
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        List<String> runtime = new ArrayList<>();
        for (String permission : requested) {
            if (isRuntimePermission(permission)) runtime.add(permission);
        }

        if (runtime.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_permissions_title)
                    .setMessage(R.string.app_permissions_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String[] labels = new String[runtime.size()];
        for (int i = 0; i < runtime.size(); i++) {
            String permission = runtime.get(i);
            boolean granted = pm.checkPermission(permission, packageName)
                    == PackageManager.PERMISSION_GRANTED;
            labels[i] = shortName(permission) + " — " + getString(granted
                    ? R.string.app_permission_granted : R.string.app_permission_denied);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_permissions_title)
                .setItems(labels, (dialog, which) -> togglePermission(runtime.get(which)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void togglePermission(@NonNull String permission) {
        if (!AppCommands.isValidPermission(permission)) return;
        PackageManager pm = getPackageManager();
        boolean granted = pm.checkPermission(permission, packageName)
                == PackageManager.PERMISSION_GRANTED;
        progress.setVisibility(View.VISIBLE);
        EXECUTOR.submit(() -> {
            AppActionResult result = AppActions.setPermission(packageName, permission, !granted);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(this, result.successful
                                ? getString(R.string.app_result_done, shortName(permission))
                                : getString(R.string.app_result_failed_plain),
                        Toast.LENGTH_SHORT).show();
                if (result.successful) showPermissions();
            });
        });
    }

    /** True for permissions the shell can grant/revoke, i.e. the dangerous runtime ones. */
    private boolean isRuntimePermission(@NonNull String permission) {
        try {
            android.content.pm.PermissionInfo info = getPackageManager().getPermissionInfo(
                    permission, 0);
            int base = info.protectionLevel
                    & android.content.pm.PermissionInfo.PROTECTION_MASK_BASE;
            return base == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @NonNull
    private static String shortName(@NonNull String permission) {
        int dot = permission.lastIndexOf('.');
        return dot < 0 ? permission : permission.substring(dot + 1);
    }

    /** Lists a small set of common AppOps and lets each mode be changed. */
    private void showAppOps() {
        String[] labels = new String[COMMON_OPS.length];
        for (int i = 0; i < COMMON_OPS.length; i++) {
            labels[i] = COMMON_OPS[i];
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_appops_title)
                .setItems(labels, (dialog, which) -> chooseAppOpMode(COMMON_OPS[which]))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseAppOpMode(@NonNull String op) {
        if (!AppCommands.isValidOp(op)) return;
        String[] modes = {
                getString(R.string.app_appops_allow),
                getString(R.string.app_appops_ignore),
                getString(R.string.app_appops_deny),
                getString(R.string.app_appops_default),
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(op)
                .setItems(modes, (dialog, which) -> {
                    String mode = AppCommands.OP_MODES.get(which);
                    progress.setVisibility(View.VISIBLE);
                    EXECUTOR.submit(() -> {
                        AppActionResult result = AppActions.setAppOp(packageName, op, mode);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            progress.setVisibility(View.GONE);
                            Toast.makeText(this, result.successful
                                            ? getString(R.string.app_result_done, op)
                                            : getString(R.string.app_result_failed_plain),
                                    Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmBattery() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_battery_title)
                .setMessage(R.string.app_battery_body)
                .setNegativeButton(R.string.app_battery_restore, (d, w) -> setBattery(false))
                .setPositiveButton(R.string.app_battery_exempt, (d, w) -> setBattery(true))
                .show();
    }

    private void setBattery(boolean whitelist) {
        progress.setVisibility(View.VISIBLE);
        EXECUTOR.submit(() -> {
            AppActionResult result = AppActions.setBatteryWhitelisted(packageName, whitelist);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(this, result.successful
                                ? getString(R.string.app_result_done,
                                getString(R.string.app_battery_title))
                                : getString(R.string.app_result_failed_plain),
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void showBuckets() {
        String[] buckets = {
                getString(R.string.app_bucket_active),
                getString(R.string.app_bucket_working_set),
                getString(R.string.app_bucket_frequent),
                getString(R.string.app_bucket_rare),
                getString(R.string.app_bucket_restricted),
                getString(R.string.app_bucket_never),
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_bucket_title)
                .setItems(buckets, (dialog, which) -> {
                    String bucket = AppCommands.STANDBY_BUCKETS.get(which);
                    progress.setVisibility(View.VISIBLE);
                    EXECUTOR.submit(() -> {
                        AppActionResult result = AppActions.setStandbyBucket(packageName, bucket);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            progress.setVisibility(View.GONE);
                            Toast.makeText(this, result.successful
                                            ? getString(R.string.app_result_done, bucket)
                                            : getString(R.string.app_result_failed_plain),
                                    Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
