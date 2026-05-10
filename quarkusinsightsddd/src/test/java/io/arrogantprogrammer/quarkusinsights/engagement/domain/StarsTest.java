package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Stars} value object.
 * Verifies all range constraints and equality semantics.
 */
class StarsTest {

    @Test
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new Stars(0));
    }

    @Test
    void acceptsOne() {
        Stars s = new Stars(1);
        assertEquals(1, s.value());
    }

    @Test
    void acceptsFive() {
        Stars s = new Stars(5);
        assertEquals(5, s.value());
    }

    @Test
    void rejectsSix() {
        assertThrows(IllegalArgumentException.class, () -> new Stars(6));
    }

    @Test
    void rejectsNegativeOne() {
        assertThrows(IllegalArgumentException.class, () -> new Stars(-1));
    }

    @Test
    void equalityByValue() {
        Stars a = new Stars(3);
        Stars b = new Stars(3);
        assertEquals(a, b);
    }
}
