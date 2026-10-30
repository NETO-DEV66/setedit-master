package io.github.netodev66.setedit.data;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Keys whose value has an outsized effect on device behaviour.
 *
 * <p>Editing these wrongly can leave the device unstable or hard to use, so the editor asks for
 * confirmation first (unless the user turned the warning off). The list is deliberately small and
 * limited to keys that are genuinely easy to get wrong.
 */
public final class RiskyKeys {
    private static final Set<String> RISKY = new HashSet<>(Arrays.asList(
            "adb_enabled",
            "adb_wifi_enabled",
            "development_settings_enabled",
            "stay_on_while_plugged_in",
            "always_finish_activities",
            "window_animation_scale",
            "transition_animation_scale",
            "animator_duration_scale",
            "screen_off_timeout",
            "pointer_location",
            "show_touches",
            "accelerometer_rotation",
            "user_rotation",
            "device_provisioned",
            "boot_count",
            "global_http_proxy_host",
            "global_http_proxy_port",
            "wifi_on",
            "bluetooth_on",
            "airplane_mode_on",
            "install_non_market_apps",
            "mock_location",
            "private_dns_mode",
            "private_dns_specifier",
            "low_power",
            "device_idle_constants"
    ));

    private RiskyKeys() {
    }

    public static boolean isRisky(@NonNull String key) {
        return RISKY.contains(key);
    }
}
