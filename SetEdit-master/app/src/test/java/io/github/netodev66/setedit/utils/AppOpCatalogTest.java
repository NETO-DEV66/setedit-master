package io.github.netodev66.setedit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Locks down the AppOps catalogue and the two parsers that read the shell's output. */
public class AppOpCatalogTest {

    @Test
    public void catalogueIsNotEmptyAndHasNoDuplicates() {
        assertTrue(AppOpCatalog.OPS.size() > 100);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (AppOpCatalog.AppOp op : AppOpCatalog.OPS) {
            assertTrue("duplicate op " + op.name, seen.add(op.name));
        }
    }

    @Test
    public void catalogueHoldsWellKnownOpsWithTheirDefaults() {
        assertEquals("allow", AppOpCatalog.defaultMode("CAMERA"));
        assertEquals("allow", AppOpCatalog.defaultMode("WAKE_LOCK"));
        assertEquals("ignore", AppOpCatalog.defaultMode("WRITE_SMS"));
        assertEquals("deny", AppOpCatalog.defaultMode("MOCK_LOCATION"));
        assertEquals("foreground", AppOpCatalog.defaultMode("TAKE_AUDIO_FOCUS"));
    }

    @Test
    public void unknownOpIsRejected() {
        assertFalse(AppOpCatalog.isKnown("NOT_A_REAL_OP"));
        assertNull(AppOpCatalog.defaultMode("NOT_A_REAL_OP"));
        assertTrue(AppOpCatalog.isKnown("VIBRATE"));
    }

    @Test
    public void parsesBarePackageLines() {
        Map<String, String> modes = AppOpCatalog.parseSetOps(Arrays.asList(
                "VIBRATE: deny",
                "WAKE_LOCK: ignore"));
        assertEquals("deny", modes.get("VIBRATE"));
        assertEquals("ignore", modes.get("WAKE_LOCK"));
        assertEquals(2, modes.size());
    }

    @Test
    public void parsesUidPrefixedLinesAndDropsTheHeader() {
        // A uid-level override prints "Uid mode: " on the first line only.
        Map<String, String> modes = AppOpCatalog.parseSetOps(Arrays.asList(
                "Uid mode: CAMERA: ignore",
                "CAMERA: allow"));
        assertEquals("ignore", modes.get("CAMERA"));
        assertEquals(1, modes.size());
    }

    @Test
    public void uidValueWinsOverPackageValue() {
        // Uid-level beats package-level, and the uid block is printed first, so the later
        // package value must not overwrite it.
        Map<String, String> modes = AppOpCatalog.parseSetOps(Arrays.asList(
                "CAMERA: ignore",
                "CAMERA: allow"));
        assertEquals("ignore", modes.get("CAMERA"));
    }

    @Test
    public void unknownNamesAndJunkAreSkipped() {
        Map<String, String> modes = AppOpCatalog.parseSetOps(Arrays.asList(
                "No operations.",
                "",
                "SOMETHING_FROM_THE_FUTURE: allow",
                "VIBRATE: deny",
                "VIBRATE"));
        assertEquals(1, modes.size());
        assertEquals("deny", modes.get("VIBRATE"));
    }

    @Test
    public void modeIsLowercased() {
        assertEquals("deny", AppOpCatalog.parseSetOps(
                Collections.singletonList("VIBRATE: DENY")).get("VIBRATE"));
    }

    @Test
    public void timeSuffixIsNotPartOfTheMode() {
        // A recently changed op prints "; time=+2m28s565ms ago" after the mode.
        Map<String, String> modes = AppOpCatalog.parseSetOps(Arrays.asList(
                "Uid mode: BLUETOOTH_CONNECT: allow; time=+27s637ms ago",
                "USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER: default; time=+2m28s565ms ago"));
        assertEquals("allow", modes.get("BLUETOOTH_CONNECT"));
        assertEquals("default", modes.get("USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER"));
    }

    @Test
    public void compatParseKeepsOnlyTheRequestedPackage() {
        List<String> dump = Arrays.asList(
                "ChangeId(136274596; name=PROCESS_CAPABILITY_CHANGE_ID; enableSinceTargetSdk=30;"
                        + " packageOverrides={com.example=true}; rawOverrides={com.example=true})",
                "ChangeId(161252188; name=ENFORCE_INTENTS_TO_MATCH_INTENT_FILTERS; disabled;"
                        + " packageOverrides={com.example=false}; rawOverrides={com.example=false};"
                        + " overridable)",
                "ChangeId(189969744; name=DOWNSCALE_65; disabled; overridable)",
                "ChangeId(297989574; name=ENABLE_FEATURE_MAPPING;"
                        + " packageOverrides={com.other=true})");
        List<CompatChange> changes = CompatChanges.parse(dump, "com.example");
        assertEquals(2, changes.size());

        CompatChange first = changes.get(0);
        assertEquals("136274596", first.id);
        assertEquals("PROCESS_CAPABILITY_CHANGE_ID", first.name);
        assertTrue(first.enabled);
        assertFalse(first.overridable);

        CompatChange second = changes.get(1);
        assertEquals("161252188", second.id);
        assertFalse(second.enabled);
        assertTrue(second.overridable);
    }

    @Test
    public void compatParseFallsBackToTheIdWhenTheNameIsMissing() {
        List<CompatChange> changes = CompatChanges.parse(
                Collections.singletonList("ChangeId(12345; packageOverrides={com.example=true})"),
                "com.example");
        assertEquals(1, changes.size());
        assertEquals("12345", changes.get(0).id);
        assertEquals("12345", changes.get(0).name);
    }

    @Test
    public void compatParseIgnoresJunkAndBadIds() {
        assertTrue(CompatChanges.parse(Arrays.asList(
                "no change id here",
                "ChangeId(0; packageOverrides={com.example=true})",
                "ChangeId(abc; packageOverrides={com.example=true})"),
                "com.example").isEmpty());
    }

    @Test
    public void compatCommandBuildersAreExact() {
        assertEquals("am compat enable --no-kill 136274596 com.example",
                AppCommands.compatSet("136274596", "com.example", true));
        assertEquals("am compat disable --no-kill 136274596 com.example",
                AppCommands.compatSet("136274596", "com.example", false));
        assertEquals("am compat reset-all --no-kill com.example",
                AppCommands.compatResetAll("com.example"));
        assertEquals("dumpsys platform_compat", AppCommands.compatDump());
    }

    @Test
    public void compatChangeValidation() {
        assertTrue(AppCommands.isValidCompatChange("136274596"));
        assertFalse(AppCommands.isValidCompatChange("0"));
        assertFalse(AppCommands.isValidCompatChange(""));
        assertFalse(AppCommands.isValidCompatChange("12; rm -rf /"));
        assertFalse(AppCommands.isValidCompatChange(null));
    }

    @Test
    public void appOpCommandsAreExact() {
        assertEquals("cmd appops get com.example", AppCommands.appOpGetAll("com.example"));
        assertEquals("cmd appops get com.example CAMERA",
                AppCommands.appOpGet("com.example", "CAMERA"));
        assertEquals("cmd appops set com.example CAMERA deny",
                AppCommands.appOpSet("com.example", "CAMERA", "deny"));
        assertEquals("cmd appops reset com.example", AppCommands.appOpReset("com.example"));
    }

    @Test
    public void foregroundIsAnAcceptedMode() {
        assertTrue(AppCommands.isValidOpMode("foreground"));
        assertEquals(5, AppCommands.OP_MODES.size());
        assertNotNull(AppCommands.OP_MODES.get(0));
    }
}
