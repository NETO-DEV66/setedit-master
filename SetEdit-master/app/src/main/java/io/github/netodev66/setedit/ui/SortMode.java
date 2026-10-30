package io.github.netodev66.setedit.ui;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.netodev66.setedit.R;

/** How a table's rows are ordered in the browser. */
public enum SortMode {
    DEFAULT(R.string.sort_default),
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    VALUE_ASC(R.string.sort_value_asc),
    VALUE_DESC(R.string.sort_value_desc);

    @StringRes
    public final int labelRes;

    SortMode(@StringRes int labelRes) {
        this.labelRes = labelRes;
    }

    /** Returns a sorted copy of {@code rows}; {@link #DEFAULT} leaves the order untouched. */
    @NonNull
    public static List<Pair<String, String>> apply(@NonNull SortMode mode,
                                                   @NonNull List<Pair<String, String>> rows) {
        List<Pair<String, String>> copy = new ArrayList<>(rows);
        if (mode == DEFAULT) return copy;
        Comparator<Pair<String, String>> comparator = comparatorFor(mode);
        Collections.sort(copy, comparator);
        return copy;
    }

    @NonNull
    private static Comparator<Pair<String, String>> comparatorFor(@NonNull SortMode mode) {
        Comparator<Pair<String, String>> byName = (a, b) ->
                compareStrings(a.first, b.first);
        Comparator<Pair<String, String>> byValue = (a, b) ->
                compareStrings(a.second, b.second);
        switch (mode) {
            case NAME_DESC:
                return byName.reversed();
            case VALUE_ASC:
                return byValue.thenComparing(byName);
            case VALUE_DESC:
                return byValue.reversed().thenComparing(byName);
            case NAME_ASC:
            default:
                return byName;
        }
    }

    /** Case-insensitive comparison that puts a missing value last in ascending order. */
    private static int compareStrings(String a, String b) {
        boolean leftMissing = a == null || a.isEmpty();
        boolean rightMissing = b == null || b.isEmpty();
        if (leftMissing || rightMissing) {
            return Boolean.compare(leftMissing, rightMissing);
        }
        return a.toLowerCase(Locale.ROOT).compareTo(b.toLowerCase(Locale.ROOT));
    }
}
