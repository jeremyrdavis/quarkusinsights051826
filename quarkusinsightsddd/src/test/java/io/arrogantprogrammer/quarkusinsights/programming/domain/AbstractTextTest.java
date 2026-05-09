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
        String tooShort = "a".repeat(99);
        assertThrows(IllegalArgumentException.class, () -> new AbstractText(tooShort));
    }

    @Test
    void acceptsExactMinimumLength() {
        String exactly100 = "a".repeat(100);
        AbstractText t = new AbstractText(exactly100);
        assertEquals(exactly100, t.value());
    }

    @Test
    void acceptsExactMaximumLength() {
        String exactly5000 = "a".repeat(5000);
        AbstractText t = new AbstractText(exactly5000);
        assertEquals(exactly5000, t.value());
    }

    @Test
    void rejectsAboveMaximumLength() {
        String tooLong = "a".repeat(5001);
        assertThrows(IllegalArgumentException.class, () -> new AbstractText(tooLong));
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
