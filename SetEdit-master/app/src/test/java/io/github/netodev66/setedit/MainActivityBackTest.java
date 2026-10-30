package io.github.netodev66.setedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.test.core.app.ActivityScenario;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.netodev66.setedit.data.AppPrefs;

/**
 * Back from a non-Home tab returns to Home; back from Home leaves the activity.
 *
 * <p>The regression this guards is that the system back button finished the shell from every tab,
 * so the user lost their place instead of stepping back to the dashboard.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityBackTest {

    @Before
    public void setUp() {
        AppPrefs.setCheckUpdates(androidx.test.core.app.ApplicationProvider
                .getApplicationContext(), false);
        AppPrefs.setAutoReconnect(androidx.test.core.app.ApplicationProvider
                .getApplicationContext(), false);
        AppPrefs.setOnboarded(androidx.test.core.app.ApplicationProvider
                .getApplicationContext(), true);
    }

    @Test
    public void backFromAnotherTabReturnsHome() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
                nav.setSelectedItemId(R.id.nav_settings);
                assertEquals(R.id.nav_settings, nav.getSelectedItemId());
            });

            scenario.onActivity(activity -> activity.getOnBackPressedDispatcher().onBackPressed());

            scenario.onActivity(activity -> {
                assertFalse(activity.isFinishing());
                BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
                assertEquals(R.id.nav_home, nav.getSelectedItemId());
            });
        }
    }

    @Test
    public void backFromHomeLeavesTheActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
                assertEquals(R.id.nav_home, nav.getSelectedItemId());
                assertTrue(nav.getVisibility() == View.VISIBLE);
            });

            scenario.onActivity(activity -> activity.getOnBackPressedDispatcher().onBackPressed());

            scenario.onActivity(activity -> assertTrue(activity.isFinishing()));
        }
    }
}
