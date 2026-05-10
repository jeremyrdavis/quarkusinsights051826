package io.arrogantprogrammer.quarkusinsights.people.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link Email} value-object validation and equality.
 */
class EmailTest {

    @Test
    void nullValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }

    @Test
    void noAtSignThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Email("notanemail.com"));
    }

    @Test
    void multipleAtSignsThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Email("a@b@c.com"));
    }

    @Test
    void blankLocalPartThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Email("@domain.com"));
    }

    @Test
    void blankDomainPartThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Email("local@"));
    }

    @Test
    void containsSpaceThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Email("local @domain.com"));
    }

    @Test
    void validEmailAccepted() {
        Email email = new Email("jane.smith@example.com");
        assertEquals("jane.smith@example.com", email.value());
    }

    @Test
    void equalityByValue() {
        Email a = new Email("user@host.io");
        Email b = new Email("user@host.io");
        assertEquals(a, b);
    }
}
