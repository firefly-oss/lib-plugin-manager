package com.catalis.core.plugin.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "1.0.0", "0.1.0", "0.0.1", "1.2.3", 
            "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-beta+build.123"
    })
    void isValidVersion_shouldReturnTrueForValidVersions(String version) {
        assertTrue(VersionUtils.isValidVersion(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "invalid", "1", "1.0", "1.0.0.0", 
            "1.0.a", "1.a.0", "a.0.0", "-1.0.0"
    })
    void isValidVersion_shouldReturnFalseForInvalidVersions(String version) {
        assertFalse(VersionUtils.isValidVersion(version));
    }

    @Test
    void isValidVersion_shouldReturnFalseForNull() {
        assertFalse(VersionUtils.isValidVersion(null));
    }

    @ParameterizedTest
    @CsvSource({
            "1.0.0,1.0.0,0",
            "1.0.0,1.0.1,-1",
            "1.0.1,1.0.0,1",
            "1.0.0,1.1.0,-1",
            "1.1.0,1.0.0,1",
            "1.0.0,2.0.0,-1",
            "2.0.0,1.0.0,1",
            "1.0.0-alpha,1.0.0,-1",
            "1.0.0,1.0.0-alpha,1",
            "1.0.0-alpha,1.0.0-beta,-1",
            "1.0.0-beta,1.0.0-alpha,1",
            "1.0.0-alpha.1,1.0.0-alpha.2,-1",
            "1.0.0-alpha.2,1.0.0-alpha.1,1",
            "1.0.0-alpha.1,1.0.0-alpha.1.0,-1",
            "1.0.0+build.1,1.0.0+build.2,0"
    })
    void compareVersions_shouldCompareVersionsCorrectly(String version1, String version2, int expected) {
        assertEquals(expected, VersionUtils.compareVersions(version1, version2));
    }

    @Test
    void compareVersions_shouldThrowExceptionForInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> VersionUtils.compareVersions("invalid", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> VersionUtils.compareVersions("1.0.0", "invalid"));
        assertThrows(IllegalArgumentException.class, () -> VersionUtils.compareVersions(null, "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> VersionUtils.compareVersions("1.0.0", null));
    }

    @ParameterizedTest
    @CsvSource({
            "1.0.0,>=1.0.0,true",
            "1.0.0,>1.0.0,false",
            "1.0.0,<1.0.0,false",
            "1.0.0,<=1.0.0,true",
            "1.0.0,=1.0.0,true",
            "1.0.0,==1.0.0,true",
            "1.0.0,!=1.0.0,false",
            "1.0.1,>1.0.0,true",
            "1.0.1,>=1.0.0,true",
            "0.9.0,<1.0.0,true",
            "0.9.0,<=1.0.0,true",
            "1.0.0-alpha,<1.0.0,true",
            "1.0.0-alpha,<=1.0.0,true"
    })
    void satisfiesConstraint_shouldCheckConstraintsCorrectly(String version, String constraint, boolean expected) {
        assertEquals(expected, VersionUtils.satisfiesConstraint(version, constraint));
    }

    @Test
    void satisfiesConstraint_shouldThrowExceptionForInvalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> VersionUtils.satisfiesConstraint("invalid", ">=1.0.0"));
    }

    @Test
    void satisfiesConstraint_shouldThrowExceptionForInvalidConstraint() {
        assertThrows(IllegalArgumentException.class, () -> VersionUtils.satisfiesConstraint("1.0.0", "invalid"));
    }

    @Test
    void sortVersions_shouldSortVersionsCorrectly() {
        String[] versions = {"1.0.0", "2.0.0", "0.5.0", "1.1.0", "1.0.1"};
        VersionUtils.sortVersions(versions);
        assertArrayEquals(new String[]{"0.5.0", "1.0.0", "1.0.1", "1.1.0", "2.0.0"}, versions);
    }

    @Test
    void sortVersions_shouldHandleNullArray() {
        assertDoesNotThrow(() -> VersionUtils.sortVersions(null));
    }

    @Test
    void getNextVersion_shouldReturnCorrectNextVersion() {
        assertEquals("2.0.0", VersionUtils.getNextVersion("1.0.0", VersionUtils.VersionUpdateType.MAJOR));
        assertEquals("1.1.0", VersionUtils.getNextVersion("1.0.0", VersionUtils.VersionUpdateType.MINOR));
        assertEquals("1.0.1", VersionUtils.getNextVersion("1.0.0", VersionUtils.VersionUpdateType.PATCH));
    }

    @Test
    void getNextVersion_shouldThrowExceptionForInvalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> VersionUtils.getNextVersion("invalid", VersionUtils.VersionUpdateType.MAJOR));
    }
}
