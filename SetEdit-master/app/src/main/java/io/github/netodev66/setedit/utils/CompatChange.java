package io.github.netodev66.setedit.utils;

import androidx.annotation.NonNull;

/**
 * One platform-compat change and the value a package has set for it.
 *
 * <p>The change id is the number {@code am compat enable|disable} takes. The name is what the
 * platform calls it and is only present on builds that expose it through {@code dumpsys}; it is
 * kept because an id alone tells the user nothing.
 */
public final class CompatChange {

    @NonNull
    public final String id;
    @NonNull
    public final String name;
    /** The value this package has: {@code true} enabled, {@code false} disabled. */
    public final boolean enabled;
    /** True when the platform marks the change as one an app may override. */
    public final boolean overridable;

    public CompatChange(@NonNull String id, @NonNull String name, boolean enabled,
                        boolean overridable) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.overridable = overridable;
    }
}
