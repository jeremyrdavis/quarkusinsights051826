package io.arrogantprogrammer.quarkusinsights.programming.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link AirDate} value-object semantics: non-null wrapping
 * a {@link LocalDate}, equality by value. Note: AirDate accepts past
 * and future dates equally — temporal predicates (e.g., "is in the
 * future") are imposed by aggregate behavior (e.g., {@code Episode.schedule})
 * not by this VO.
 */
class AirDateTest {

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new AirDate(null));
    }

    @Test
    void acceptsAnyLocalDate() {
        LocalDate d = LocalDate.of(2026, 6, 1);
        AirDate a = new AirDate(d);
        assertEquals(d, a.value());
    }

    @Test
    void acceptsPastDate() {
        AirDate a = new AirDate(LocalDate.of(2000, 1, 1));
        assertEquals(LocalDate.of(2000, 1, 1), a.value());
    }

    @Test
    void equalsByValue() {
        AirDate a = new AirDate(LocalDate.of(2026, 6, 1));
        AirDate b = new AirDate(LocalDate.of(2026, 6, 1));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
