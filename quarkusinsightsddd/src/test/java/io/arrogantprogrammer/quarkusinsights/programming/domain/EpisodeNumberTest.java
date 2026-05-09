package io.arrogantprogrammer.quarkusinsights.programming.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link EpisodeNumber} value-object semantics: the wrapped
 * primitive {@code int} must be at least 1 (zero and negatives are
 * rejected), and equality is by value.
 */
class EpisodeNumberTest {

    @Test
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeNumber(0));
    }

    @Test
    void rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeNumber(-1));
    }

    @Test
    void acceptsOne() {
        EpisodeNumber n = new EpisodeNumber(1);
        assertEquals(1, n.value());
    }

    @Test
    void acceptsLargeValues() {
        EpisodeNumber n = new EpisodeNumber(10_000);
        assertEquals(10_000, n.value());
    }

    @Test
    void equalsByValue() {
        assertEquals(new EpisodeNumber(42), new EpisodeNumber(42));
        assertEquals(new EpisodeNumber(42).hashCode(), new EpisodeNumber(42).hashCode());
    }
}
