package io.arrogantprogrammer.quarkusinsights.programming.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link AbstractText} value-object semantics: non-null,
 * non-blank, length 100..5000 chars, equality by value.
 */
class AbstractTextTest {

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new AbstractText(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> new AbstractText("   ".repeat(50)));
    }

    @Test
    void rejectsBelowMinimumLength() {
        String too_short = "a".repeat(99);
        assertThrows(IllegalArgumentException.class, () -> new AbstractText(too_short));
    }

    @Test
    void acceptsExactMinimumLength() {
        String exactly_100 = "a".repeat(100);
        AbstractText t = new AbstractText(exactly_100);
        assertEquals(exactly_100, t.value());
    }

    @Test
    void acceptsExactMaximumLength() {
        String exactly_5000 = "a".repeat(5000);
        AbstractText t = new AbstractText(exactly_5000);
        assertEquals(exactly_5000, t.value());
    }

    @Test
    void rejectsAboveMaximumLength() {
        String too_long = "a".repeat(5001);
        assertThrows(IllegalArgumentException.class, () -> new AbstractText(too_long));
    }

    @Test
    void equalsByValue() {
        String content = "x".repeat(150);
        AbstractText a = new AbstractText(content);
        AbstractText b = new AbstractText(content);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
