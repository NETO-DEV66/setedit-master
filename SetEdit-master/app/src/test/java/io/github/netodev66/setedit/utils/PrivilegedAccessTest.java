package io.github.netodev66.setedit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.netodev66.setedit.data.AppPrefs;

/**
 * Tests for the state that keeps a one-time connect from having to be repeated: the remembered ADB
 * endpoint, the paired flag and the "privilege was granted once" flag.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PrivilegedAccessTest {

    @Test
    public void adbEndpointIsRememberedAndCleared() {
        Context context = ApplicationProvider.getApplicationContext();
        AppPrefs.clearAdbEndpoint(context);
        assertNull(AppPrefs.getAdbHost(context));
        assertEquals(-1, AppPrefs.getAdbPort(context));

        AppPrefs.setAdbEndpoint(context, "192.168.1.5", 37099);
        assertEquals("192.168.1.5", AppPrefs.getAdbHost(context));
        assertEquals(37099, AppPrefs.getAdbPort(context));

        AppPrefs.clearAdbEndpoint(context);
        assertNull(AppPrefs.getAdbHost(context));
    }

    @Test
    public void endpointIgnoresInvalidValues() {
        Context context = ApplicationProvider.getApplicationContext();
        AppPrefs.clearAdbEndpoint(context);
        AppPrefs.setAdbEndpoint(context, null, 0);
        assertNull(AppPrefs.getAdbHost(context));
        AppPrefs.setAdbEndpoint(context, "10.0.0.1", -1);
        assertNull(AppPrefs.getAdbHost(context));
    }

    @Test
    public void pairingAndGrantFlagsPersist() {
        Context context = ApplicationProvider.getApplicationContext();
        AppPrefs.setAdbPaired(context, false);
        AppPrefs.setPrivilegeGrantedOnce(context, false);
        assertFalse(AppPrefs.isAdbPaired(context));
        assertFalse(AppPrefs.isPrivilegeGrantedOnce(context));

        AppPrefs.setAdbPaired(context, true);
        AppPrefs.setPrivilegeGrantedOnce(context, true);
        assertTrue(AppPrefs.isAdbPaired(context));
        assertTrue(AppPrefs.isPrivilegeGrantedOnce(context));
    }

    @Test
    public void shizukuDenialIsRemembered() {
        Context context = ApplicationProvider.getApplicationContext();
        AppPrefs.setShizukuDenied(context, true);
        assertTrue(AppPrefs.isShizukuDenied(context));
        AppPrefs.setShizukuDenied(context, false);
        assertFalse(AppPrefs.isShizukuDenied(context));
    }

    @Test
    public void noAccessMeansSetupIsNeeded() {
        Context context = ApplicationProvider.getApplicationContext();
        AppPrefs.setAdbPaired(context, false);
        AppPrefs.setPrivilegeGrantedOnce(context, false);
        // With nothing granted and no root/Shizuku in a plain JVM test, setup is required.
        assertTrue(PrivilegedAccess.needsUserSetup(context));
    }

    @Test
    public void pairedDeviceDoesNotNeedSetupAgain() {
        Context context = ApplicationProvider.getApplicationContext();
        AppPrefs.setAdbPaired(context, true);
        assertFalse(PrivilegedAccess.needsUserSetup(context));
        assertTrue(PrivilegedAccess.hasEverHadAccess(context));
    }
}
