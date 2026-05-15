package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Sequential identifier for an episode within the QuarkusInsights program.
 *
 * <p>A Value Object — immutable, equals-by-value. Episodes are numbered
 * starting at 1, in the order they are scheduled. Numbers are unique
 * across the program (enforced at the use-case and database level in
 * later plans, not here in the domain layer where uniqueness across an
 * aggregate set cannot be checked).
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param value the episode number; must be at least 1
 */
public record EpisodeNumber(int value) {

    /**
     * Creates an EpisodeNumber.
     *
     * @param value the episode number; must be at least 1
     * @throws IllegalArgumentException if {@code value} is less than 1
     */
    public EpisodeNumber {
        if (value < 1) {
            throw new IllegalArgumentException("EpisodeNumber must be at least 1, got: " + value);
        }
    }
}
