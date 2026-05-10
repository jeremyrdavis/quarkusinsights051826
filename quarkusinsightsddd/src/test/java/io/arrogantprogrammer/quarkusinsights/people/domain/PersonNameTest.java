package io.arrogantprogrammer.quarkusinsights.people.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link PersonName} value-object validation and behavior.
 */
class PersonNameTest {

    @Test
    void nullFirstThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new PersonName(null, "Smith"));
    }

    @Test
    void nullLastThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new PersonName("Jane", null));
    }

    @Test
    void blankFirstThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new PersonName("  ", "Smith"));
    }

    @Test
    void blankLastThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new PersonName("Jane", "  "));
    }

    @Test
    void singleCharBothAccepted() {
        PersonName name = new PersonName("J", "S");
        assertEquals("J", name.first());
        assertEquals("S", name.last());
    }

    @Test
    void exactlyOneHundredCharsAccepted() {
        String maxFirst = "A".repeat(100);
        String maxLast = "B".repeat(100);
        PersonName name = new PersonName(maxFirst, maxLast);
        assertEquals(maxFirst, name.first());
        assertEquals(maxLast, name.last());
    }

    @Test
    void oneHundredAndOneCharsFirstThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new PersonName("A".repeat(101), "Smith"));
    }

    @Test
    void oneHundredAndOneCharsLastThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new PersonName("Jane", "B".repeat(101)));
    }

    @Test
    void equalityByValue() {
        PersonName a = new PersonName("Jane", "Smith");
        PersonName b = new PersonName("Jane", "Smith");
        assertEquals(a, b);
    }

    @Test
    void displayNameIsFirstSpaceLast() {
        PersonName name = new PersonName("Jane", "Smith");
        assertEquals("Jane Smith", name.displayName());
    }
}
