package io.github.netodev66.setedit.utils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code dumpsys platform_compat} into the changes a single package overrides.
 *
 * <p>The dump prints one {@code ChangeId(...)} block per change, of the form:
 *
 * <pre>
 * ChangeId(136274596; name=PROCESS_CAPABILITY_CHANGE_ID; enableSinceTargetSdk=30;
 *     packageOverrides={com.android.chrome=true}; rawOverrides={com.android.chrome=true})
 * ChangeId(161252188; name=ENFORCE_INTENTS_TO_MATCH_INTENT_FILTERS; disabled;
 *     packageOverrides={com.android.chrome=false}; rawOverrides={...}; overridable)
 * </pre>
 *
 * <p>Only the {@code packageOverrides} entry for the requested package matters: it is the value
 * {@code am compat} reads and writes. Changes the package does not override are skipped, so the
 * list is exactly the set {@code am compat reset-all} would clear. The change id is validated
 * before it is kept, because it is interpolated into a shell line later.
 */
public final class CompatChanges {

    private static final Pattern CHANGE = Pattern.compile(
            "ChangeId\\(\\s*([0-9]{1,19})\\s*;[^)]*?\\)");
    private static final Pattern NAME = Pattern.compile("name=([A-Za-z0-9_]+)");
    private static final Pattern OVERRIDABLE = Pattern.compile(";\\s*overridable\\b");

    private CompatChanges() {
    }

    @NonNull
    public static List<CompatChange> parse(@NonNull List<String> lines, @NonNull String pkg) {
        List<CompatChange> changes = new ArrayList<>();
        for (String line : lines) {
            Matcher block = CHANGE.matcher(line);
            if (!block.find()) continue;
            String id = block.group(1);
            if (!AppCommands.isValidCompatChange(id)) continue;
            // The package entry lives inside this block; a change that only lists other packages
            // does not affect the app being viewed.
            Matcher value = Pattern.compile(
                    Pattern.quote(pkg) + "=(true|false)").matcher(line);
            if (!value.find()) continue;
            Matcher name = NAME.matcher(line);
            changes.add(new CompatChange(id,
                    name.find() ? name.group(1) : id,
                    "true".equals(value.group(1)),
                    OVERRIDABLE.matcher(line).find()));
        }
        return changes;
    }
}
