package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new RatingId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID uuid = UUID.fromString("77777777-7777-7777-7777-777777777777");
        RatingId a = new RatingId(uuid);
        RatingId b = new RatingId(uuid);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        RatingId a = RatingId.random();
        RatingId b = RatingId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "88888888-8888-8888-8888-888888888888";
        RatingId id = RatingId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> RatingId.fromString("not-a-uuid"));
    }
}
