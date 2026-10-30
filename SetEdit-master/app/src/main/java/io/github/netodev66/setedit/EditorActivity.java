package io.github.netodev66.setedit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.github.netodev66.setedit.adapters.AbsRecyclerAdapter;
import io.github.netodev66.setedit.adapters.AdapterProvider;
import io.github.netodev66.setedit.adapters.SettingsRecyclerAdapter;
import io.github.netodev66.setedit.adb.AdbConnector;
import io.github.netodev66.setedit.adb.ConnectActivity;
import io.github.netodev66.setedit.boot.ActionItem;
import io.github.netodev66.setedit.boot.BootUtils;
import io.github.netodev66.setedit.shortcut.ShortcutUtils;
import io.github.netodev66.setedit.utils.ActionResult;
import io.github.netodev66.setedit.utils.ShellExecutor;
import io.github.netodev66.util.UiUtils;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;
import rikka.shizuku.Shizuku;

public class EditorActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        SearchView.OnQueryTextListener {
    private static final String SELECTED_TABLE = "SELECTED_TABLE";
    private static final String CONNECT_PROMPT_SHOWN = "connect_prompt_shown";
    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;

    @NonNull
    private final AdapterProvider adapterProvider = new AdapterProvider(this);

    @Nullable
    private AppCompatSpinner spinnerTable;
    @Nullable
    private SearchView searchView;
    private ExtendedFloatingActionButton addNewItem;
    private AbsRecyclerAdapter adapter;
    private RecyclerView listView;
    private SharedPreferences preferences;
    @Nullable
    private Timer refreshTimer;

    /** Opens the wireless-debugging connect screen and refreshes once it succeeds. */
    private final ActivityResultLauncher<Intent> connectLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && adapter != null) adapter.refresh();
            });

    /** Refreshes the visible table once Shizuku grants or revokes access. */
    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener =
            (requestCode, grantResult) -> {
                if (requestCode != SHIZUKU_PERMISSION_REQUEST_CODE) return;
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.privileged_access_granted, Toast.LENGTH_SHORT).show();
                    if (adapter != null) adapter.refresh();
                } else {
                    Toast.makeText(this, R.string.privileged_access_denied, Toast.LENGTH_SHORT).show();
                }
            };

    private final ActivityResultLauncher<String> post21SaveLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri == null) return;
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new IOException();
                    saveAsJson(os);
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                } catch (Throwable th) {
                    th.printStackTrace();
                    Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
                }
            });

    private void displayOneTimeWarningDialog() {
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean hasWarned = preferences.getBoolean("has_warned", false);
        if (hasWarned) return;
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.startup_warning)
                .setNegativeButton(R.string.close, null)
                .show();
        preferences.edit().putBoolean("has_warned", true).apply();
    }

    public void addNewItemDialog() {
        View editorDialogView = getLayoutInflater().inflate(R.layout.dialog_new, null);
        EditText keyNameView = editorDialogView.findViewById(R.id.txtName);
        EditText keyValueView = editorDialogView.findViewById(R.id.txtValue);
        MaterialCheckBox performOnReboot = editorDialogView.findViewById(R.id.checkbox);
        MaterialCheckBox performViaShortcut = editorDialogView.findViewById(R.id.checkbox_2);
        if (adapter.canSetOnReboot()) {
            performOnReboot.setVisibility(View.VISIBLE);
        } else performOnReboot.setVisibility(View.GONE);
        if (adapter.canCreateShortcut()) {
            performViaShortcut.setVisibility(View.VISIBLE);
        } else performViaShortcut.setVisibility(View.GONE);
        keyNameView.requestFocus();
        new MaterialAlertDialogBuilder(this)
                .setView(editorDialogView)
                .setTitle(R.string.new_item)
                .setPositiveButton(R.string.save, ((dialog, which) -> {
                    Editable keyName = keyNameView.getText();
                    Editable keyValue = keyValueView.getText();
                    if (TextUtils.isEmpty(keyName) || keyValue == null) return;
                    adapter.create(keyName.toString(), keyValue.toString());
                    if (adapter.canSetOnReboot() && performOnReboot.isChecked()) {
                        ActionItem actionItem = new ActionItem(ActionResult.TYPE_CREATE, EditorUtils.toTableType(adapter.getListType()), keyName.toString(), keyValue.toString());
                        BootUtils.add(this, actionItem);
                    }
                    if (adapter.canCreateShortcut() && performViaShortcut.isChecked()) {
                        ActionItem actionItem = new ActionItem(ActionResult.TYPE_CREATE, EditorUtils.toTableType(adapter.getListType()), keyName.toString(), keyValue.toString());
                        ShortcutUtils.displayShortcutTypeChooserDialog(this, actionItem);
                    }
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onCreate(Bundle bundle) {
        preferences = getSharedPreferences("prefs", MODE_PRIVATE);
        int mode = preferences.getInt("theme", AppCompatDelegate.getDefaultNightMode());
        AppCompatDelegate.setDefaultNightMode(mode);
        super.onCreate(bundle);
        setContentView(R.layout.activity_editor);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.toolbar_custom_view);
            View actionBarView = actionBar.getCustomView();
            // Item view
            spinnerTable = actionBarView.findViewById(R.id.spinner);
            spinnerTable.setOnItemSelectedListener(this);
            spinnerTable.setAdapter(ArrayAdapter.createFromResource(this, R.array.settings_table, R.layout.item_spinner));
            animateToolbar(actionBarView);
        }
        // List view
        listView = findViewById(R.id.recycler_view);
        listView.setLayoutManager(new LinearLayoutManager(this));
        new FastScrollerBuilder(listView).useMd2Style().build();
        // Add efab
        addNewItem = findViewById(R.id.efab);
        addNewItem.setOnClickListener(v -> {
            if (adapter instanceof SettingsRecyclerAdapter) {
                if (ShellExecutor.isAvailable()
                        || EditorUtils.canWriteSettings(this,
                        ((SettingsRecyclerAdapter) adapter).getSettingsType())) {
                    addNewItemDialog();
                }
            }
        });
        UiUtils.applyWindowInsetsAsMargin(addNewItem);
        UiUtils.applyWindowInsetsAsPaddingNoTop(listView);
        // Display warning if it's the first time
        displayOneTimeWarningDialog();
        // Offer the one-time wireless-debugging connection on the first launch, and reconnect
        // silently afterwards if a key was already paired.
        maybeConnect();
        // Poll for external settings changes. The timer is cancelled in onDestroy; until the
        // spinner reports its first selection the adapter is still null, so skip those ticks.
        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || adapter == null) return;
                    adapter.refresh();
                });
            }
        }, 5000, 5000);
    }

    /**
     * Makes sure a privileged shell is on its way.
     *
     * <p>Root and Shizuku need nothing from the user. Otherwise the app's own ADB connection is
     * attempted in the background; when it needs pairing and the user has not been asked yet, the
     * connect screen is opened. The prompt is shown at most once so a device without wireless
     * debugging does not nag on every launch.
     */
    private void maybeConnect() {
        if (ShellExecutor.isAvailable()) return;
        AdbConnector.runAsync(() -> {
            AdbConnector.State state = AdbConnector.connect(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (state == AdbConnector.State.CONNECTED) {
                    if (adapter != null) adapter.refresh();
                    return;
                }
                if (preferences.getBoolean(CONNECT_PROMPT_SHOWN, false)) return;
                preferences.edit().putBoolean(CONNECT_PROMPT_SHOWN, true).apply();
                openConnectScreen();
            });
        });
    }

    private void openConnectScreen() {
        connectLauncher.launch(new Intent(this, ConnectActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
        } catch (Throwable ignored) {
        }
        if (adapter != null) adapter.refresh();
    }

    @Override
    protected void onPause() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
        } catch (Throwable ignored) {
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        super.onDestroy();
    }

    /**
     * Explains the privileged-access requirement and offers the available way to get it: root and
     * Shizuku are used automatically, otherwise the user is taken to the wireless-debugging screen.
     */
    private void showPrivilegedAccessDialog() {
        ShellExecutor.Mode mode = ShellExecutor.getMode();
        String status;
        boolean canRequestShizuku = false;
        boolean canConnect = false;
        if (mode == ShellExecutor.Mode.ROOT) {
            status = getString(R.string.privileged_access_root);
        } else if (mode == ShellExecutor.Mode.SHIZUKU) {
            status = getString(R.string.privileged_access_shizuku_ready);
        } else if (mode == ShellExecutor.Mode.ADB) {
            status = getString(R.string.privileged_access_adb_ready);
        } else if (ShellExecutor.isShizukuBinderAvailable()) {
            status = getString(R.string.privileged_access_shizuku_installed);
            canRequestShizuku = true;
        } else {
            status = getString(R.string.privileged_access_shizuku_missing);
            canConnect = true;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.privileged_access_title)
                .setMessage(status + "\n\n" + getString(R.string.privileged_access_message))
                .setNegativeButton(R.string.close, null);
        if (canRequestShizuku) {
            builder.setPositiveButton(R.string.grant, (dialog, which) -> requestShizukuPermission());
        } else if (canConnect) {
            builder.setPositiveButton(R.string.connect_button, (dialog, which) -> openConnectScreen());
        }
        builder.show();
    }

    private void requestShizukuPermission() {
        try {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
        } catch (Throwable th) {
            Toast.makeText(this, R.string.privileged_access_shizuku_missing, Toast.LENGTH_SHORT).show();
        }
    }

    /** Slides the toolbar chooser in from the top with a springy settle. */
    private void animateToolbar(@NonNull View toolbarView) {
        toolbarView.setAlpha(0f);
        toolbarView.setTranslationY(-UiUtils.dpToPx(this, 12));
        toolbarView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(380L)
                .setInterpolator(AnimationUtils.loadInterpolator(this, R.interpolator.m3_spring_bouncy))
                .start();
    }

    /** Fades and lifts the list in when the selected table changes. */
    private void animateListIn() {
        listView.setAlpha(0f);
        listView.setTranslationY(UiUtils.dpToPx(this, 8));
        listView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280L)
                .setInterpolator(AnimationUtils.loadInterpolator(this, R.interpolator.m3_spring_bouncy))
                .start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_editor_actions, menu);
        // Search view
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // Set query listener
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_export) {
            post21SaveLauncher.launch(getFileName());
            return true;
        } else if (id == R.id.action_theme) {
            List<Integer> themeMap = new ArrayList<>(4);
            // Sequence must be preserved
            themeMap.add(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            themeMap.add(AppCompatDelegate.MODE_NIGHT_NO);
            themeMap.add(AppCompatDelegate.MODE_NIGHT_YES);
            themeMap.add(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            int mode = preferences.getInt("theme", AppCompatDelegate.getDefaultNightMode());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.theme)
                    .setSingleChoiceItems(R.array.theme_options, themeMap.indexOf(mode), (dialog, which) -> {
                        int newMode = themeMap.get(which);
                        preferences.edit().putInt("theme", newMode).apply();
                        AppCompatDelegate.setDefaultNightMode(newMode);
                        dialog.dismiss();
                    })
                    .show();
        } else if (id == R.id.action_privileged_access) {
            showPrivilegedAccessDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        listView.setAdapter(adapter = adapterProvider.getRecyclerAdapter(position));
        listView.setItemAnimator(new DefaultItemAnimator());
        animateListIn();
        if (adapter.canCreate()) {
            addNewItem.show();
        } else addNewItem.hide();
        if (searchView != null) {
            searchView.setQuery(null, false);
            searchView.clearFocus();
            searchView.setIconified(true);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        addNewItem.show();
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle bundle) {
        if (spinnerTable != null) {
            spinnerTable.setSelection(bundle.getInt(SELECTED_TABLE));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (spinnerTable != null) {
            bundle.putInt(SELECTED_TABLE, spinnerTable.getSelectedItemPosition());
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (adapter != null) {
            adapter.filter(newText.toLowerCase(Locale.ROOT));
        }
        return false;
    }

    private String getFileName() {
        return "SetEdit-" + System.currentTimeMillis() + ".json";
    }

    private void saveAsJson(OutputStream os) throws JSONException, IOException {
        String jsonString = EditorUtils.getJson(adapter.getAllItems(), adapter instanceof SettingsRecyclerAdapter ?
                ((SettingsRecyclerAdapter) adapter).getSettingsType() : null);
        os.write(jsonString.getBytes());
    }
}
