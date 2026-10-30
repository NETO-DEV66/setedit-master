package io.github.netodev66.setedit.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Classifies a settings value so the editor can present the right control. */
public final class ValueType {
    public enum Type {TEXT, BOOLEAN, NUMBER}

    private ValueType() {
    }

    @NonNull
    public static Type of(@Nullable String value) {
        if (value == null) return Type.TEXT;
        String v = value.trim();
        if (v.isEmpty()) return Type.TEXT;
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) return Type.BOOLEAN;
        if (v.equals("0") || v.equals("1")) return Type.BOOLEAN;
        return isNumber(v) ? Type.NUMBER : Type.TEXT;
    }

    public static boolean isNumber(@NonNull String v) {
        try {
            Long.parseLong(v);
            return true;
        } catch (NumberFormatException ignored) {
        }
        try {
            Double.parseDouble(v);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Adjusts a numeric string by {@code delta}, keeping the value an integer when it was one. */
    @NonNull
    public static String adjust(@NonNull String current, long delta) {
        String trimmed = current.trim();
        try {
            long base = Long.parseLong(trimmed);
            return String.valueOf(base + delta);
        } catch (NumberFormatException ignored) {
        }
        try {
            double base = Double.parseDouble(trimmed);
            double result = base + delta;
            if (result == Math.rint(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (NumberFormatException ignored) {
            return current;
        }
    }
}
