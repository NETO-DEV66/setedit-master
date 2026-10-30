package io.github.netodev66.setedit.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionComparatorTest {

    @Test
    public void newerMajor() {
        assertTrue(VersionComparator.isNewer("5.0", "4.1"));
    }

    @Test
    public void sameVersionIsNotNewer() {
        assertFalse(VersionComparator.isNewer("4.1", "4.1"));
    }

    @Test
    public void olderVersionIsNotNewer() {
        assertFalse(VersionComparator.isNewer("4.0", "4.1"));
    }

    @Test
    public void numericNotLexicographic() {
        // 4.10 must beat 4.9, which a string compare would get backwards.
        assertTrue(VersionComparator.isNewer("4.10", "4.9"));
        assertFalse(VersionComparator.isNewer("4.9", "4.10"));
    }

    @Test
    public void missingPartsCountAsZero() {
        assertTrue(VersionComparator.isNewer("4.1.1", "4.1"));
        assertFalse(VersionComparator.isNewer("4.1", "4.1.0"));
        assertTrue(VersionComparator.isNewer("5", "4.9.9"));
    }

    @Test
    public void leadingVIsIgnored() {
        assertTrue(VersionComparator.isNewer("v4.2", "4.1"));
        assertFalse(VersionComparator.isNewer("V4.1", "4.1"));
    }

    @Test
    public void buildSuffixIsIgnored() {
        assertFalse(VersionComparator.isNewer("4.1-beta1", "4.1"));
        assertTrue(VersionComparator.isNewer("4.2+22", "4.1"));
    }

    @Test
    public void normalizeStripsPrefixAndSuffix() {
        assertEquals("4.1", VersionComparator.normalize("v4.1"));
        assertEquals("4.1", VersionComparator.normalize("4.1-beta"));
        assertEquals("4.1", VersionComparator.normalize("  v4.1+build  "));
    }
}
