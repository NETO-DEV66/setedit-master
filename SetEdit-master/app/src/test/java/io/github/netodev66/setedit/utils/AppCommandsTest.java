package io.github.netodev66.setedit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Locks down the exact shell lines the app manager issues, and the input validation. */
public class AppCommandsTest {

    @Test
    public void forceStopUsesAm() {
        assertEquals("am force-stop com.example", AppCommands.forceStop("com.example"));
    }

    @Test
    public void clearCacheKeepsData() {
        assertEquals("pm clear --cache-only com.example", AppCommands.clearCache("com.example"));
        assertEquals("pm clear com.example", AppCommands.clearData("com.example"));
    }

    @Test
    public void systemUninstallTargetsUserZero() {
        assertEquals("pm uninstall --user 0 com.example",
                AppCommands.uninstallForUser("com.example"));
        assertEquals("pm uninstall com.example", AppCommands.uninstall("com.example"));
    }

    @Test
    public void disableAndEnableUseDisableUser() {
        assertEquals("pm disable-user --user 0 com.example", AppCommands.disable("com.example"));
        assertEquals("pm enable com.example", AppCommands.enable("com.example"));
    }

    @Test
    public void permissionsGrantAndRevoke() {
        assertEquals("pm grant com.example android.permission.CAMERA",
                AppCommands.grant("com.example", "android.permission.CAMERA"));
        assertEquals("pm revoke com.example android.permission.CAMERA",
                AppCommands.revoke("com.example", "android.permission.CAMERA"));
    }

    @Test
    public void appOpsSetCarriesMode() {
        assertEquals("cmd appops set com.example WAKE_LOCK deny",
                AppCommands.appOpSet("com.example", "WAKE_LOCK", "deny"));
        assertEquals("cmd appops get com.example WAKE_LOCK",
                AppCommands.appOpGet("com.example", "WAKE_LOCK"));
    }

    @Test
    public void batteryWhitelistAddsAndRemoves() {
        assertEquals("dumpsys deviceidle whitelist +com.example",
                AppCommands.batteryWhitelist("com.example", true));
        assertEquals("dumpsys deviceidle whitelist -com.example",
                AppCommands.batteryWhitelist("com.example", false));
    }

    @Test
    public void standbyBucketRoundTrip() {
        assertEquals("am get-standby-bucket com.example", AppCommands.standbyBucketGet("com.example"));
        assertEquals("am set-standby-bucket com.example rare",
                AppCommands.standbyBucketSet("com.example", "rare"));
    }

    @Test
    public void packageValidationAcceptsRealNames() {
        assertTrue(AppCommands.isValidPackage("com.example.app"));
        assertTrue(AppCommands.isValidPackage("android"));
        assertTrue(AppCommands.isValidPackage("io.github.netodev66.setedit"));
    }

    @Test
    public void packageValidationRejectsInjection() {
        assertFalse(AppCommands.isValidPackage("com.example; rm -rf /"));
        assertFalse(AppCommands.isValidPackage("com.example && reboot"));
        assertFalse(AppCommands.isValidPackage("com.example app"));
        assertFalse(AppCommands.isValidPackage("com/example"));
        assertFalse(AppCommands.isValidPackage(null));
        assertFalse(AppCommands.isValidPackage(""));
    }

    @Test
    public void opAndModeValidation() {
        assertTrue(AppCommands.isValidOp("WAKE_LOCK"));
        assertFalse(AppCommands.isValidOp("wake lock"));
        assertTrue(AppCommands.isValidOpMode("deny"));
        assertFalse(AppCommands.isValidOpMode("destroy"));
        assertTrue(AppCommands.isValidBucket("restricted"));
        assertFalse(AppCommands.isValidBucket("turbo"));
    }
}
