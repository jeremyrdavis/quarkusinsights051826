package io.arrogantprogrammer.quarkusinsights.programming.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link Abstract} inside-aggregate entity semantics:
 * non-null id, text, and timestamp; equality by value of all
 * components.
 */
class AbstractTest {

    private static final AbstractText sampleText =
        new AbstractText("a".repeat(100));

    @Test
    void rejectsNullId() {
        assertThrows(IllegalArgumentException.class,
            () -> new Abstract(null, sampleText, Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Test
    void rejectsNullText() {
        assertThrows(IllegalArgumentException.class,
            () -> new Abstract(AbstractId.random(), null, Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Test
    void rejectsNullSubmittedAt() {
        assertThrows(IllegalArgumentException.class,
            () -> new Abstract(AbstractId.random(), sampleText, null));
    }

    @Test
    void equalsByAllComponents() {
        AbstractId id = AbstractId.random();
        Instant when = Instant.parse("2026-01-01T00:00:00Z");
        Abstract a = new Abstract(id, sampleText, when);
        Abstract b = new Abstract(id, sampleText, when);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differsByText() {
        AbstractId id = AbstractId.random();
        Instant when = Instant.parse("2026-01-01T00:00:00Z");
        Abstract a = new Abstract(id, sampleText, when);
        Abstract b = new Abstract(id, new AbstractText("b".repeat(100)), when);
        assertNotEquals(a, b);
    }
}
