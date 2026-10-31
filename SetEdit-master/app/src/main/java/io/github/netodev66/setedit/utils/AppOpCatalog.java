package io.github.netodev66.setedit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The AppOps catalogue and the parser for {@code cmd appops get}.
 *
 * <p>The shell has no way to enumerate ops: {@code cmd appops} offers only {@code get}, {@code set},
 * {@code query-op} and {@code reset}, and none of them lists the known op names. The names are
 * therefore mirrored from AOSP's {@code AppOpsManager.sAppOpInfos} (the array whose order is the op
 * code order) and carry each op's platform default mode. Both were taken from the
 * {@code android16-release} branch and cross-checked against a real Android 16 device, where every
 * name below was accepted by {@code cmd appops get}.
 *
 * <p>Reading is done with a single {@code cmd appops get <pkg>} call, which prints only the ops a
 * package has moved away from their default. One op at a time would take about four minutes for the
 * whole catalogue, so it is never done per row.
 */
public final class AppOpCatalog {

    /** Prefix {@code cmd appops get} prints on the uid block when a uid-level override exists. */
    private static final String UID_PREFIX = "Uid mode: ";

    /** One AppOps operation: its shell name and the mode it takes when nothing has been set. */
    public static final class AppOp {
        @NonNull
        public final String name;
        @NonNull
        public final String defaultMode;

        AppOp(@NonNull String name, @NonNull String defaultMode) {
            this.name = name;
            this.defaultMode = defaultMode;
        }
    }

    /** Every op known to AOSP, in op-code order. */
    @NonNull
    public static final List<AppOp> OPS;

    static {
        List<AppOp> ops = new ArrayList<>(162);
        Collections.addAll(ops,
            new AppOp("COARSE_LOCATION", "allow"),
            new AppOp("FINE_LOCATION", "allow"),
            new AppOp("GPS", "allow"),
            new AppOp("VIBRATE", "allow"),
            new AppOp("READ_CONTACTS", "allow"),
            new AppOp("WRITE_CONTACTS", "allow"),
            new AppOp("READ_CALL_LOG", "allow"),
            new AppOp("WRITE_CALL_LOG", "allow"),
            new AppOp("READ_CALENDAR", "allow"),
            new AppOp("WRITE_CALENDAR", "allow"),
            new AppOp("WIFI_SCAN", "allow"),
            new AppOp("POST_NOTIFICATION", "allow"),
            new AppOp("NEIGHBORING_CELLS", "allow"),
            new AppOp("CALL_PHONE", "allow"),
            new AppOp("READ_SMS", "allow"),
            new AppOp("WRITE_SMS", "ignore"),
            new AppOp("RECEIVE_SMS", "allow"),
            new AppOp("RECEIVE_EMERGENCY_BROADCAST", "allow"),
            new AppOp("RECEIVE_MMS", "allow"),
            new AppOp("RECEIVE_WAP_PUSH", "allow"),
            new AppOp("SEND_SMS", "allow"),
            new AppOp("READ_ICC_SMS", "allow"),
            new AppOp("WRITE_ICC_SMS", "allow"),
            new AppOp("WRITE_SETTINGS", "allow"),
            new AppOp("SYSTEM_ALERT_WINDOW", "allow"),
            new AppOp("ACCESS_NOTIFICATIONS", "allow"),
            new AppOp("CAMERA", "allow"),
            new AppOp("RECORD_AUDIO", "allow"),
            new AppOp("PLAY_AUDIO", "allow"),
            new AppOp("READ_CLIPBOARD", "allow"),
            new AppOp("WRITE_CLIPBOARD", "allow"),
            new AppOp("TAKE_MEDIA_BUTTONS", "allow"),
            new AppOp("TAKE_AUDIO_FOCUS", "foreground"),
            new AppOp("AUDIO_MASTER_VOLUME", "allow"),
            new AppOp("AUDIO_VOICE_VOLUME", "allow"),
            new AppOp("AUDIO_RING_VOLUME", "allow"),
            new AppOp("AUDIO_MEDIA_VOLUME", "allow"),
            new AppOp("AUDIO_ALARM_VOLUME", "allow"),
            new AppOp("AUDIO_NOTIFICATION_VOLUME", "allow"),
            new AppOp("AUDIO_BLUETOOTH_VOLUME", "allow"),
            new AppOp("WAKE_LOCK", "allow"),
            new AppOp("MONITOR_LOCATION", "allow"),
            new AppOp("MONITOR_HIGH_POWER_LOCATION", "allow"),
            new AppOp("GET_USAGE_STATS", "allow"),
            new AppOp("MUTE_MICROPHONE", "allow"),
            new AppOp("TOAST_WINDOW", "allow"),
            new AppOp("PROJECT_MEDIA", "ignore"),
            new AppOp("ACTIVATE_VPN", "ignore"),
            new AppOp("WRITE_WALLPAPER", "allow"),
            new AppOp("ASSIST_STRUCTURE", "allow"),
            new AppOp("ASSIST_SCREENSHOT", "allow"),
            new AppOp("READ_PHONE_STATE", "allow"),
            new AppOp("ADD_VOICEMAIL", "allow"),
            new AppOp("USE_SIP", "allow"),
            new AppOp("PROCESS_OUTGOING_CALLS", "allow"),
            new AppOp("USE_FINGERPRINT", "allow"),
            new AppOp("BODY_SENSORS", "allow"),
            new AppOp("READ_CELL_BROADCASTS", "allow"),
            new AppOp("MOCK_LOCATION", "deny"),
            new AppOp("READ_EXTERNAL_STORAGE", "allow"),
            new AppOp("WRITE_EXTERNAL_STORAGE", "allow"),
            new AppOp("TURN_SCREEN_ON", "default"),
            new AppOp("GET_ACCOUNTS", "allow"),
            new AppOp("RUN_IN_BACKGROUND", "allow"),
            new AppOp("AUDIO_ACCESSIBILITY_VOLUME", "allow"),
            new AppOp("READ_PHONE_NUMBERS", "allow"),
            new AppOp("REQUEST_INSTALL_PACKAGES", "allow"),
            new AppOp("PICTURE_IN_PICTURE", "allow"),
            new AppOp("INSTANT_APP_START_FOREGROUND", "allow"),
            new AppOp("ANSWER_PHONE_CALLS", "allow"),
            new AppOp("RUN_ANY_IN_BACKGROUND", "allow"),
            new AppOp("CHANGE_WIFI_STATE", "allow"),
            new AppOp("REQUEST_DELETE_PACKAGES", "allow"),
            new AppOp("BIND_ACCESSIBILITY_SERVICE", "allow"),
            new AppOp("ACCEPT_HANDOVER", "allow"),
            new AppOp("MANAGE_IPSEC_TUNNELS", "deny"),
            new AppOp("START_FOREGROUND", "allow"),
            new AppOp("BLUETOOTH_SCAN", "allow"),
            new AppOp("USE_BIOMETRIC", "allow"),
            new AppOp("ACTIVITY_RECOGNITION", "allow"),
            new AppOp("SMS_FINANCIAL_TRANSACTIONS", "allow"),
            new AppOp("READ_MEDIA_AUDIO", "allow"),
            new AppOp("WRITE_MEDIA_AUDIO", "deny"),
            new AppOp("READ_MEDIA_VIDEO", "allow"),
            new AppOp("WRITE_MEDIA_VIDEO", "deny"),
            new AppOp("READ_MEDIA_IMAGES", "allow"),
            new AppOp("WRITE_MEDIA_IMAGES", "deny"),
            new AppOp("LEGACY_STORAGE", "allow"),
            new AppOp("ACCESS_ACCESSIBILITY", "allow"),
            new AppOp("READ_DEVICE_IDENTIFIERS", "deny"),
            new AppOp("ACCESS_MEDIA_LOCATION", "allow"),
            new AppOp("QUERY_ALL_PACKAGES", "allow"),
            new AppOp("MANAGE_EXTERNAL_STORAGE", "allow"),
            new AppOp("INTERACT_ACROSS_PROFILES", "allow"),
            new AppOp("ACTIVATE_PLATFORM_VPN", "ignore"),
            new AppOp("LOADER_USAGE_STATS", "allow"),
            new AppOp("AUTO_REVOKE_PERMISSIONS_IF_UNUSED", "allow"),
            new AppOp("AUTO_REVOKE_MANAGED_BY_INSTALLER", "allow"),
            new AppOp("NO_ISOLATED_STORAGE", "deny"),
            new AppOp("PHONE_CALL_MICROPHONE", "allow"),
            new AppOp("PHONE_CALL_CAMERA", "allow"),
            new AppOp("RECORD_AUDIO_HOTWORD", "allow"),
            new AppOp("MANAGE_ONGOING_CALLS", "allow"),
            new AppOp("MANAGE_CREDENTIALS", "allow"),
            new AppOp("USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER", "allow"),
            new AppOp("RECORD_AUDIO_OUTPUT", "allow"),
            new AppOp("SCHEDULE_EXACT_ALARM", "allow"),
            new AppOp("FINE_LOCATION_SOURCE", "allow"),
            new AppOp("COARSE_LOCATION_SOURCE", "allow"),
            new AppOp("MANAGE_MEDIA", "allow"),
            new AppOp("BLUETOOTH_CONNECT", "allow"),
            new AppOp("UWB_RANGING", "allow"),
            new AppOp("ACTIVITY_RECOGNITION_SOURCE", "allow"),
            new AppOp("BLUETOOTH_ADVERTISE", "allow"),
            new AppOp("RECORD_INCOMING_PHONE_AUDIO", "allow"),
            new AppOp("NEARBY_WIFI_DEVICES", "allow"),
            new AppOp("ESTABLISH_VPN_SERVICE", "allow"),
            new AppOp("ESTABLISH_VPN_MANAGER", "allow"),
            new AppOp("ACCESS_RESTRICTED_SETTINGS", "default"),
            new AppOp("RECEIVE_SOUNDTRIGGER_AUDIO", "allow"),
            new AppOp("RECEIVE_EXPLICIT_USER_INTERACTION_AUDIO", "allow"),
            new AppOp("RUN_USER_INITIATED_JOBS", "allow"),
            new AppOp("READ_MEDIA_VISUAL_USER_SELECTED", "allow"),
            new AppOp("SYSTEM_EXEMPT_FROM_SUSPENSION", "allow"),
            new AppOp("SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS", "allow"),
            new AppOp("READ_WRITE_HEALTH_DATA", "allow"),
            new AppOp("FOREGROUND_SERVICE_SPECIAL_USE", "allow"),
            new AppOp("SYSTEM_EXEMPT_FROM_POWER_RESTRICTIONS", "allow"),
            new AppOp("SYSTEM_EXEMPT_FROM_HIBERNATION", "allow"),
            new AppOp("SYSTEM_EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION", "allow"),
            new AppOp("CAPTURE_CONSENTLESS_BUGREPORT_ON_USERDEBUG_BUILD", "allow"),
            new AppOp("DEPRECATED_2", "ignore"),
            new AppOp("USE_FULL_SCREEN_INTENT", "allow"),
            new AppOp("CAMERA_SANDBOXED", "allow"),
            new AppOp("RECORD_AUDIO_SANDBOXED", "allow"),
            new AppOp("RECEIVE_SANDBOX_TRIGGER_AUDIO", "default"),
            new AppOp("DEPRECATED_3", "ignore"),
            new AppOp("CREATE_ACCESSIBILITY_OVERLAY", "allow"),
            new AppOp("MEDIA_ROUTING_CONTROL", "allow"),
            new AppOp("ENABLE_MOBILE_DATA_BY_USER", "allow"),
            new AppOp("OP_RESERVED_FOR_TESTING", "allow"),
            new AppOp("RAPID_CLEAR_NOTIFICATIONS_BY_LISTENER", "allow"),
            new AppOp("READ_SYSTEM_GRAMMATICAL_GENDER", "allow"),
            new AppOp("DEPRECATED_4", "ignore"),
            new AppOp("ARCHIVE_ICON_OVERLAY", "allow"),
            new AppOp("UNARCHIVAL_CONFIRMATION", "allow"),
            new AppOp("EMERGENCY_LOCATION", "allow"),
            new AppOp("RECEIVE_SENSITIVE_NOTIFICATIONS", "ignore"),
            new AppOp("READ_HEART_RATE", "allow"),
            new AppOp("READ_SKIN_TEMPERATURE", "allow"),
            new AppOp("RANGING", "allow"),
            new AppOp("READ_OXYGEN_SATURATION", "allow"),
            new AppOp("WRITE_SYSTEM_PREFERENCES", "allow"),
            new AppOp("CONTROL_AUDIO", "foreground"),
            new AppOp("CONTROL_AUDIO_PARTIAL", "foreground"),
            new AppOp("EYE_TRACKING_COARSE", "allow"),
            new AppOp("EYE_TRACKING_FINE", "allow"),
            new AppOp("FACE_TRACKING", "allow"),
            new AppOp("HAND_TRACKING", "allow"),
            new AppOp("HEAD_TRACKING", "allow"),
            new AppOp("SCENE_UNDERSTANDING_COARSE", "allow"),
            new AppOp("SCENE_UNDERSTANDING_FINE", "allow")
        );
        OPS = Collections.unmodifiableList(ops);
    }

    @NonNull
    private static final Map<String, String> BY_NAME;

    static {
        Map<String, String> byName = new LinkedHashMap<>();
        for (AppOp op : OPS) {
            byName.put(op.name, op.defaultMode);
        }
        BY_NAME = Collections.unmodifiableMap(byName);
    }

    private AppOpCatalog() {
    }

    /** The platform default mode for an op, or {@code null} when the name is not in the catalogue. */
    @Nullable
    public static String defaultMode(@NonNull String name) {
        return BY_NAME.get(name);
    }

    /** True when the name is a real op, so a bad value never reaches the shell. */
    public static boolean isKnown(@NonNull String name) {
        return BY_NAME.containsKey(name);
    }

    /**
     * Parses {@code cmd appops get <pkg>} into op name to mode.
     *
     * <p>A package that also has uid-level overrides prints a {@code Uid mode: } prefix on the first
     * line and then bare {@code OP: mode} lines. Uid-level wins over package-level, and the uid
     * block is printed first, so a later duplicate must not overwrite the earlier one. Only the
     * catalogue names are kept, which drops the uid header and any future op this build predates.
     */
    @NonNull
    public static Map<String, String> parseSetOps(@NonNull List<String> lines) {
        Map<String, String> modes = new LinkedHashMap<>();
        for (String line : lines) {
            // The uid block is introduced by a "Uid mode: " marker on the line itself.
            String entry = line.startsWith(UID_PREFIX)
                    ? line.substring(UID_PREFIX.length())
                    : line;
            int colon = entry.lastIndexOf(':');
            if (colon <= 0) continue;
            String name = entry.substring(0, colon).trim();
            String raw = entry.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
            // A set op may carry a "; time=+2m ago" suffix; the mode is only the leading word.
            int semicolon = raw.indexOf(';');
            String mode = semicolon >= 0 ? raw.substring(0, semicolon).trim() : raw;
            if (!isKnown(name)) continue;
            if (!modes.containsKey(name)) {
                modes.put(name, mode);
            }
        }
        return modes;
    }
}
