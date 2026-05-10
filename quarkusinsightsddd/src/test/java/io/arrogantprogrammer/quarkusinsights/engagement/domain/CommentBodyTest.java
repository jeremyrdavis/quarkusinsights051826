package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link CommentBody} value object.
 * Verifies all validation constraints and equality semantics.
 */
class CommentBodyTest {

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new CommentBody(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new CommentBody("   "));
    }

    @Test
    void rejectsEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> new CommentBody(""));
    }

    @Test
    void acceptsLengthOne() {
        CommentBody b = new CommentBody("x");
        assertEquals("x", b.value());
    }

    @Test
    void acceptsLengthTwoThousand() {
        String twoK = "a".repeat(2000);
        CommentBody b = new CommentBody(twoK);
        assertEquals(twoK, b.value());
    }

    @Test
    void rejectsLengthTwoThousandAndOne() {
        assertThrows(IllegalArgumentException.class, () -> new CommentBody("a".repeat(2001)));
    }

    @Test
    void equalityByValue() {
        CommentBody a = new CommentBody("Great episode!");
        CommentBody b = new CommentBody("Great episode!");
        assertEquals(a, b);
    }
}
