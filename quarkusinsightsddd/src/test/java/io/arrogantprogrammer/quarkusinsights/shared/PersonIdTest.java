package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new PersonId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID uuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        PersonId a = new PersonId(uuid);
        PersonId b = new PersonId(uuid);
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
}
