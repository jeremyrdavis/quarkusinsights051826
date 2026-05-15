package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link CommentId} value-object semantics: null rejection in
 * the canonical constructor, equality and hash code based on the wrapped
 * UUID's value (not object identity), distinctness of randomly-generated
 * IDs, round-tripping via {@link CommentId#fromString(String)}, and
 * rejection of malformed and null strings by that factory.
 */
class CommentIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new CommentId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID first = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID second = UUID.fromString("55555555-5555-5555-5555-555555555555");
        // Distinct UUID objects, same value — UUID.fromString does not intern.
        CommentId a = new CommentId(first);
        CommentId b = new CommentId(second);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        CommentId a = CommentId.random();
        CommentId b = CommentId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "66666666-6666-6666-6666-666666666666";
        CommentId id = CommentId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> CommentId.fromString("not-a-uuid"));
    }

    @Test
    void fromStringRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> CommentId.fromString(null));
    }
}
