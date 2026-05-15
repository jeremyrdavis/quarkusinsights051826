package io.arrogantprogrammer.quarkusinsights.programming.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link EpisodeTitle} value-object semantics: non-null,
 * non-blank, length 1..200, equality by value.
 */
class EpisodeTitleTest {

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeTitle(null));
    }

    @Test
    void rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeTitle(""));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeTitle("   "));
    }

    @Test
    void acceptsTrimmedShortString() {
        EpisodeTitle t = new EpisodeTitle("Pilot");
        assertEquals("Pilot", t.value());
    }

    @Test
    void acceptsTwoHundredCharacters() {
        String twoHundredAs = "a".repeat(200);
        EpisodeTitle t = new EpisodeTitle(twoHundredAs);
        assertEquals(twoHundredAs, t.value());
    }

    @Test
    void rejectsTwoHundredOneCharacters() {
        String tooLong = "a".repeat(201);
        assertThrows(IllegalArgumentException.class, () -> new EpisodeTitle(tooLong));
    }

    @Test
    void equalsByValue() {
        assertEquals(new EpisodeTitle("Hello"), new EpisodeTitle("Hello"));
        assertEquals(new EpisodeTitle("Hello").hashCode(), new EpisodeTitle("Hello").hashCode());
    }
}
