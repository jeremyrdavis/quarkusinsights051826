package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link RatingId} value-object semantics: null rejection in
 * the canonical constructor, equality and hash code based on the wrapped
 * UUID's value (not object identity), distinctness of randomly-generated
 * IDs, round-tripping via {@link RatingId#fromString(String)}, and
 * rejection of malformed and null strings by that factory.
 */
class RatingIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new RatingId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID first = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID second = UUID.fromString("77777777-7777-7777-7777-777777777777");
        // Distinct UUID objects, same value — UUID.fromString does not intern.
        RatingId a = new RatingId(first);
        RatingId b = new RatingId(second);
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

    @Test
    void fromStringRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> RatingId.fromString(null));
    }
}
