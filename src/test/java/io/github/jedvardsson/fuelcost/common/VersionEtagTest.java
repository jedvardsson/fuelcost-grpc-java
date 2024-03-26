package io.github.jedvardsson.fuelcost.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionEtagTest {


    @Test
    void testToString() {
        assertEquals("W/\"5\"", VersionEtag.of(5).toString());
    }

    @Test
    void testParse_Ok() {
        assertEquals(5, VersionEtag.parse("W/\"5\"").version());
    }

    @Test
    void testParse_Fail() {
        assertThrows(IllegalArgumentException.class, () -> VersionEtag.parse("W/\"abc\""));
    }

    @Test
    void testParseOptionalVersion_Ok() {
        assertEquals(5L, VersionEtag.parseOptionalVersion("W/\"5\"").orElse(null));
    }

    @Test
    void testParseOptionalVersion_Fail() {
        assertThrows(IllegalArgumentException.class, () -> VersionEtag.parseOptionalVersion("W/\"abc\""));
    }

    @Test
    void testParseTryParseVersion_Ok() {
        assertEquals(5L, VersionEtag.tryParseVersion("W/\"5\""));
    }

    @Test
    void testParseTryParseVersion_Missing() {
        assertNull(VersionEtag.tryParseVersion(""));
        assertNull(VersionEtag.tryParseVersion(null));
    }

    @Test
    void testParseTryParseVersion_Invalid() {
        assertEquals(-1L, VersionEtag.tryParseVersion("W/\"abc\""));
    }

}