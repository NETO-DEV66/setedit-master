package io.github.netodev66.setedit.ui;

import static org.junit.Assert.assertEquals;

import androidx.core.util.Pair;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Pins the ordering each sort mode produces, including the tie-breaks. */
public class SortModeTest {

    private static List<Pair<String, String>> rows() {
        List<Pair<String, String>> rows = new ArrayList<>();
        rows.add(new Pair<>("beta", "2"));
        rows.add(new Pair<>("Alpha", "10"));
        rows.add(new Pair<>("gamma", "1"));
        return rows;
    }

    @Test
    public void defaultKeepsInsertionOrder() {
        List<Pair<String, String>> sorted = SortMode.apply(SortMode.DEFAULT, rows());
        assertEquals(Arrays.asList("beta", "Alpha", "gamma"), names(sorted));
    }

    @Test
    public void nameAscendingIsCaseInsensitive() {
        List<Pair<String, String>> sorted = SortMode.apply(SortMode.NAME_ASC, rows());
        assertEquals(Arrays.asList("Alpha", "beta", "gamma"), names(sorted));
    }

    @Test
    public void nameDescendingReversesTheNameOrder() {
        List<Pair<String, String>> sorted = SortMode.apply(SortMode.NAME_DESC, rows());
        assertEquals(Arrays.asList("gamma", "beta", "Alpha"), names(sorted));
    }

    @Test
    public void valueAscendingComparesTextNotNumbers() {
        // Values sort as strings, so "10" precedes "2" — the same order the shell prints them in.
        List<Pair<String, String>> sorted = SortMode.apply(SortMode.VALUE_ASC, rows());
        assertEquals(Arrays.asList("gamma", "Alpha", "beta"), names(sorted));
    }

    @Test
    public void valueDescendingReversesTheValueOrder() {
        List<Pair<String, String>> sorted = SortMode.apply(SortMode.VALUE_DESC, rows());
        assertEquals(Arrays.asList("beta", "Alpha", "gamma"), names(sorted));
    }

    @Test
    public void sortDoesNotMutateTheInput() {
        List<Pair<String, String>> original = rows();
        SortMode.apply(SortMode.NAME_ASC, original);
        assertEquals(Arrays.asList("beta", "Alpha", "gamma"), names(original));
    }

    @Test
    public void nullValuesSortLastInAscendingOrder() {
        List<Pair<String, String>> rows = new ArrayList<>();
        rows.add(new Pair<>("b", null));
        rows.add(new Pair<>("a", "1"));
        List<Pair<String, String>> sorted = SortMode.apply(SortMode.VALUE_ASC, rows);
        assertEquals(Arrays.asList("a", "b"), names(sorted));
    }

    private static List<String> names(List<Pair<String, String>> rows) {
        List<String> names = new ArrayList<>();
        for (Pair<String, String> row : rows) names.add(row.first);
        return names;
    }
}
