package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link AuthorHandle} value object.
 * Verifies all validation constraints and equality semantics.
 */
class AuthorHandleTest {

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new AuthorHandle(null));
    }

    @Test
    void rejectsLengthOne() {
        assertThrows(IllegalArgumentException.class, () -> new AuthorHandle("a"));
    }

    @Test
    void acceptsLengthTwo() {
        AuthorHandle h = new AuthorHandle("ab");
        assertEquals("ab", h.value());
    }

    @Test
    void acceptsLengthForty() {
        String fortyChars = "a".repeat(40);
        AuthorHandle h = new AuthorHandle(fortyChars);
        assertEquals(fortyChars, h.value());
    }

    @Test
    void rejectsLengthFortyOne() {
        assertThrows(IllegalArgumentException.class, () -> new AuthorHandle("a".repeat(41)));
    }

    @Test
    void rejectsSpace() {
        assertThrows(IllegalArgumentException.class, () -> new AuthorHandle("ab cd"));
    }

    @Test
    void rejectsExclamationMark() {
        assertThrows(IllegalArgumentException.class, () -> new AuthorHandle("ab!cd"));
    }

    @Test
    void acceptsUnderscoreAndHyphen() {
        AuthorHandle h = new AuthorHandle("aZ_09-");
        assertEquals("aZ_09-", h.value());
    }

    @Test
    void acceptsAllValidCharTypes() {
        // mix of upper, lower, digits, underscore, hyphen at boundaries
        AuthorHandle h = new AuthorHandle("Ab");
        assertEquals("Ab", h.value());
    }

    @Test
    void equalityByValue() {
        AuthorHandle a = new AuthorHandle("alice");
        AuthorHandle b = new AuthorHandle("alice");
        assertEquals(a, b);
    }
}
