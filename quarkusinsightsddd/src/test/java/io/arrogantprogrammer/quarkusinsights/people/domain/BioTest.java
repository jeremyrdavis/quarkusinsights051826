package io.arrogantprogrammer.quarkusinsights.people.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link Bio} value-object validation and equality.
 */
class BioTest {

    private static final String MIN_BIO = "A".repeat(50);
    private static final String MAX_BIO = "A".repeat(2000);

    @Test
    void nullValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Bio(null));
    }

    @Test
    void blankValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Bio("   "));
    }

    @Test
    void belowMinimumThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Bio("A".repeat(49)));
    }

    @Test
    void exactlyMinimumAccepted() {
        Bio bio = new Bio(MIN_BIO);
        assertEquals(MIN_BIO, bio.value());
    }

    @Test
    void exactlyMaximumAccepted() {
        Bio bio = new Bio(MAX_BIO);
        assertEquals(MAX_BIO, bio.value());
    }

    @Test
    void aboveMaximumThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Bio("A".repeat(2001)));
    }

    @Test
    void equalityByValue() {
        Bio a = new Bio(MIN_BIO);
        Bio b = new Bio(MIN_BIO);
        assertEquals(a, b);
    }
}
