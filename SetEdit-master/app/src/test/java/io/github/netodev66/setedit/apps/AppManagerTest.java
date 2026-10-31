package io.github.netodev66.setedit.apps;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.AppListLoader;

/**
 * Smoke tests for the app manager: the list and detail screens inflate, and the loader returns the
 * packages Robolectric exposes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppManagerTest {

    @Test
    public void appsListInflates() {
        ActivityController<AppsActivity> controller =
                Robolectric.buildActivity(AppsActivity.class).setup();
        assertNotNull(controller.get().findViewById(R.id.apps_list));
        controller.close();
    }

    @Test
    public void appDetailRejectsMalformedPackage() {
        Intent intent = AppDetailActivity.intent(ApplicationProvider.getApplicationContext(),
                "com.example; reboot");
        ActivityController<AppDetailActivity> controller =
                Robolectric.buildActivity(AppDetailActivity.class, intent).setup();
        // A bad package name must finish the screen instead of running anything.
        assertTrue(controller.get().isFinishing());
        controller.close();
    }

    @Test
    public void appDetailInflatesForSelfPackage() {
        Intent intent = AppDetailActivity.intent(ApplicationProvider.getApplicationContext(),
                ApplicationProvider.getApplicationContext().getPackageName());
        ActivityController<AppDetailActivity> controller =
                Robolectric.buildActivity(AppDetailActivity.class, intent).setup();
        assertNotNull(controller.get().findViewById(R.id.app_detail_actions));
        controller.close();
    }

    @Test
    public void loaderReturnsListWithoutThrowing() {
        assertNotNull(AppListLoader.load(ApplicationProvider.getApplicationContext()));
    }
}
