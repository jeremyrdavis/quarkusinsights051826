package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link PersonId} value-object semantics: null rejection in
 * the canonical constructor, equality and hash code based on the wrapped
 * UUID's value (not object identity), distinctness of randomly-generated
 * IDs, round-tripping via {@link PersonId#fromString(String)}, and
 * rejection of malformed and null strings by that factory.
 */
class PersonIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new PersonId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID first = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID second = UUID.fromString("33333333-3333-3333-3333-333333333333");
        // Distinct UUID objects, same value — UUID.fromString does not intern.
        PersonId a = new PersonId(first);
        PersonId b = new PersonId(second);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        PersonId a = PersonId.random();
        PersonId b = PersonId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "44444444-4444-4444-4444-444444444444";
        PersonId id = PersonId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> PersonId.fromString("not-a-uuid"));
    }

    @Test
    void fromStringRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PersonId.fromString(null));
    }
}
