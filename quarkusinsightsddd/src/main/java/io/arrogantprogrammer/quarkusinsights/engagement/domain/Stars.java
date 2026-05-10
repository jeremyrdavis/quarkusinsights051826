package io.arrogantprogrammer.quarkusinsights.engagement.domain;

/**
 * Value Object representing a star rating (1 through 5 inclusive) given
 * by an audience member to a published episode.
 *
 * <p>Constraints enforced in the canonical constructor:
 * <ul>
 *   <li>value must be in the range [1, 5] inclusive</li>
 * </ul>
 *
 * <p>Part of the Engagement bounded context, domain layer.
 *
 * @param value the star count; must be between 1 and 5 inclusive
 */
public record Stars(int value) {

    private static final int MIN_STARS = 1;
    private static final int MAX_STARS = 5;

    /**
     * Creates a Stars value object after range-checking.
     *
     * @param value the star count; must be in [1, 5]
     * @throws IllegalArgumentException if {@code value} is less than 1 or greater than 5
     */
    public Stars {
        if (value < MIN_STARS || value > MAX_STARS) {
            throw new IllegalArgumentException(
                "Stars must be between " + MIN_STARS + " and " + MAX_STARS
                    + " inclusive, was: " + value);
        }
    }
}
