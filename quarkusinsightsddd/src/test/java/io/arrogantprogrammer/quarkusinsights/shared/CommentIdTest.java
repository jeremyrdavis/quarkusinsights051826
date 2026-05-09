package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new CommentId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID uuid = UUID.fromString("55555555-5555-5555-5555-555555555555");
        CommentId a = new CommentId(uuid);
        CommentId b = new CommentId(uuid);
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
}
