package io.arrogantprogrammer.quarkusinsights.programming.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link AbstractId} value-object semantics: null rejection
 * in the canonical constructor, equality by value of the wrapped UUID,
 * distinctness of randomly-generated IDs, round-tripping via
 * {@link AbstractId#fromString(String)}, and rejection of malformed
 * and null strings by that factory.
 */
class AbstractIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new AbstractId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID first = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID second = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        AbstractId a = new AbstractId(first);
        AbstractId b = new AbstractId(second);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        assertNotEquals(AbstractId.random(), AbstractId.random());
    }

    @Test
    void fromStringRoundTrips() {
        String s = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        assertEquals(s, AbstractId.fromString(s).value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> AbstractId.fromString("not-a-uuid"));
    }

    @Test
    void fromStringRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> AbstractId.fromString(null));
    }
}
