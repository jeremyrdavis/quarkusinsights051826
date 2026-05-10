package io.arrogantprogrammer.quarkusinsights.people.domain;

/**
 * Biographical description of a Person.
 *
 * <p>A Value Object — immutable, equals-by-value. Length must be
 * 50..2000 characters (inclusive) and the content must contain at least
 * one non-whitespace character. The lower bound ensures a meaningful
 * biography; the upper bound keeps profiles concise.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param value the biography text; must be non-null, non-blank,
 *     length 50..2000 chars
 */
public record Bio(String value) {

    private static final int MIN_LENGTH = 50;
    private static final int MAX_LENGTH = 2000;

    /**
     * Creates a Bio.
     *
     * @param value the biography text
     * @throws IllegalArgumentException if {@code value} is null, blank,
     *     shorter than 50 characters, or longer than 2000 characters
     */
    public Bio {
        if (value == null) {
            throw new IllegalArgumentException("Bio must not be null");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Bio must not be blank");
        }
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Bio must be at least " + MIN_LENGTH
                + " characters, got: " + value.length());
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Bio must not exceed " + MAX_LENGTH
                + " characters, got: " + value.length());
        }
    }
}
