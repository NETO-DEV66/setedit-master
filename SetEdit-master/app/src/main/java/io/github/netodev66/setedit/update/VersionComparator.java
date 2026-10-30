package io.github.netodev66.setedit.update;

import androidx.annotation.NonNull;

/**
 * Compares dotted version strings numerically, so {@code 4.10} is correctly newer than
 * {@code 4.9} (a plain string compare would get that backwards).
 */
public final class VersionComparator {

    private VersionComparator() {
    }

    /** Strips a leading {@code v}/{@code V} and any build suffix so versions compare cleanly. */
    @NonNull
    public static String normalize(@NonNull String raw) {
        String value = raw.trim();
        if (value.startsWith("v") || value.startsWith("V")) value = value.substring(1);
        int plus = value.indexOf('+');
        if (plus > 0) value = value.substring(0, plus);
        int dash = value.indexOf('-');
        if (dash > 0) value = value.substring(0, dash);
        return value.trim();
    }

    /** True when {@code candidate} is a strictly newer dotted version than {@code current}. */
    public static boolean isNewer(@NonNull String candidate, @NonNull String current) {
        int[] a = parse(normalize(candidate));
        int[] b = parse(normalize(current));
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int left = i < a.length ? a[i] : 0;
            int right = i < b.length ? b[i] : 0;
            if (left != right) return left > right;
        }
        return false;
    }

    @NonNull
    private static int[] parse(@NonNull String version) {
        String[] parts = version.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
}
