package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Body text of an episode's abstract — the synopsis written for an
 * upcoming or past show.
 *
 * <p>A Value Object — immutable, equals-by-value. Length must be
 * 100..5000 characters (inclusive) and the content must contain at
 * least one non-whitespace character. The lower bound exists because a
 * meaningful abstract requires substance; the upper bound exists to
 * keep public-facing pages compact.
 *
 * @param value the abstract text; must be non-null, non-blank,
 *     length 100..5000 chars
 */
public record AbstractText(String value) {

    private static final int MIN_LENGTH = 100;
    private static final int MAX_LENGTH = 5000;

    /**
     * Creates an AbstractText.
     *
     * @param value the abstract text
     * @throws IllegalArgumentException if {@code value} is null,
     *     blank, shorter than 100 chars, or longer than 5000 chars
     */
    public AbstractText {
        if (value == null) {
            throw new IllegalArgumentException("AbstractText must not be null");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("AbstractText must not be blank");
        }
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "AbstractText must be at least " + MIN_LENGTH
                + " characters, got: " + value.length());
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "AbstractText must not exceed " + MAX_LENGTH
                + " characters, got: " + value.length());
        }
    }
}
