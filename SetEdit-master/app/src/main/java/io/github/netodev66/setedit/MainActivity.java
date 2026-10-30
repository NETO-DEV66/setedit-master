package io.github.netodev66.setedit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import io.github.netodev66.setedit.adb.AdbConnector;
import io.github.netodev66.setedit.adb.ConnectActivity;
import io.github.netodev66.setedit.data.AppPrefs;
import io.github.netodev66.setedit.ui.BackupFragment;
import io.github.netodev66.setedit.ui.FavoritesFragment;
import io.github.netodev66.setedit.ui.HomeFragment;
import io.github.netodev66.setedit.ui.SearchFragment;
import io.github.netodev66.setedit.ui.SettingsFragment;
import io.github.netodev66.setedit.ui.TableActivity;
import io.github.netodev66.setedit.ui.Tables;
import io.github.netodev66.setedit.update.UpdateActivity;
import io.github.netodev66.setedit.update.UpdateInfo;
import io.github.netodev66.setedit.update.UpdateManager;
import io.github.netodev66.setedit.utils.ShellExecutor;
import io.github.netodev66.util.UiUtils;

/**
 * The app shell: a bottom navigation bar over five destinations.
 *
 * <p>Home lists the tables and quick actions; the other tabs are search, favorites, backup and
 * settings. It also performs the silent reconnect on resume and opens the onboarding the first time.
 */
public class MainActivity extends AppCompatActivity implements HomeFragment.Host {

    private static final String STATE_TAB = "tab";

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        // The window is edge-to-edge; keep the bar clear of the system navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bottom);
            return insets;
        });

        int tab = savedInstanceState == null
                ? R.id.nav_home : savedInstanceState.getInt(STATE_TAB, R.id.nav_home);

        // Show the initial destination explicitly: setSelectedItemId does not fire the listener
        // when the id is already the menu's default selection, which would leave the host empty.
        if (savedInstanceState == null) {
            show(fragmentFor(tab));
        }
        bottomNav.setSelectedItemId(tab);
        // Attach the listener after the initial selection so it is not re-shown needlessly.
        bottomNav.setOnItemSelectedListener(item -> {
            bounce(item.getItemId());
            return show(fragmentFor(item.getItemId()));
        });

        UiUtils.playBouncyScreenEntrance(findViewById(R.id.nav_host));

        if (!AppPrefs.isOnboarded(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
        maybeReconnect();
        maybeCheckForUpdates();
    }

    /** Looks for a newer release at most once a day, and offers it without blocking startup. */
    private void maybeCheckForUpdates() {
        if (!AppPrefs.isCheckUpdates(this)) return;
        long last = AppPrefs.getLastUpdateCheck(this);
        long dayMillis = 24L * 60L * 60L * 1000L;
        if (System.currentTimeMillis() - last < dayMillis) return;
        UpdateManager.check(this, info -> {
            AppPrefs.setLastUpdateCheck(this, System.currentTimeMillis());
            if (info.status == UpdateInfo.Status.AVAILABLE) {
                offerUpdate(info);
            }
        });
    }

    private void offerUpdate(@NonNull UpdateInfo info) {
        if (isFinishing() || isDestroyed()) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.update_available_title, info.version))
                .setMessage(R.string.update_available_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.update_download, (d, w) ->
                        startActivity(UpdateActivity.intent(this)))
                .show();
    }

    /** Springs the selected tab's icon so a tap feels acknowledged. */
    private void bounce(int menuId) {
        View item = bottomNav.findViewById(menuId);
        if (item == null) return;
        item.setScaleX(0.85f);
        item.setScaleY(0.85f);
        item.animate().scaleX(1f).scaleY(1f)
                .setDuration(280L)
                .setInterpolator(UiUtils.bouncy(this))
                .start();
    }

    @NonNull
    private Fragment fragmentFor(int menuId) {
        if (menuId == R.id.nav_search) return new SearchFragment();
        if (menuId == R.id.nav_favorites) return new FavoritesFragment();
        if (menuId == R.id.nav_backup) return new BackupFragment();
        if (menuId == R.id.nav_settings) return new SettingsFragment();
        return new HomeFragment();
    }

    private boolean show(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.nav_enter, R.anim.nav_exit)
                .replace(R.id.nav_host, fragment)
                .commit();
        return true;
    }

    /** Selects a bottom-nav tab from a fragment, e.g. the dashboard's "Backup" quick action. */
    @Override
    public void openTab(int menuId) {
        bottomNav.setSelectedItemId(menuId);
    }

    @Override
    public void openTable(@TableTypeInt int tableType) {
        startActivity(TableActivity.intent(this, tableType));
    }

    @Override
    public void openRecipes() {
        startActivity(new Intent(this, RecipesActivity.class));
    }

    @Override
    public void openConnect() {
        startActivity(new Intent(this, ConnectActivity.class));
    }

    /** Silently reconnects wireless debugging in the background when it was set up before. */
    private void maybeReconnect() {
        if (!AppPrefs.isAutoReconnect(this)) return;
        if (ShellExecutor.isAvailable()) return;
        AdbConnector.runAsync(() -> AdbConnector.connect(getApplicationContext()));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNav != null) outState.putInt(STATE_TAB, bottomNav.getSelectedItemId());
    }
}
