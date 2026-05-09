package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Display title for an episode.
 *
 * <p>A Value Object — immutable, equals-by-value. Non-null, non-blank,
 * length 1..200 characters (the constructor rejects null, empty,
 * whitespace-only, or strings exceeding 200 characters).
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param value the title text; must be non-null, non-blank,
 *     length 1..200 chars
 */
public record EpisodeTitle(String value) {

    /**
     * Creates an EpisodeTitle.
     *
     * @param value the title text
     * @throws IllegalArgumentException if {@code value} is null,
     *     blank, or longer than 200 characters
     */
    public EpisodeTitle {
        if (value == null) {
            throw new IllegalArgumentException("EpisodeTitle must not be null");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("EpisodeTitle must not be blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException(
                "EpisodeTitle must not exceed 200 characters, got: " + value.length());
        }
    }
}
