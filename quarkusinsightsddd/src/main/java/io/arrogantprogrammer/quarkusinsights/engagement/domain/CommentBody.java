package io.arrogantprogrammer.quarkusinsights.engagement.domain;

/**
 * Value Object representing the textual body of a comment submitted by
 * an audience member on a published episode.
 *
 * <p>Constraints enforced in the canonical constructor:
 * <ul>
 *   <li>non-null</li>
 *   <li>non-blank (not empty or whitespace-only)</li>
 *   <li>length between 1 and 2000 characters inclusive</li>
 * </ul>
 *
 * <p>Part of the Engagement bounded context, domain layer.
 *
 * @param value the comment text; must satisfy the constraints above
 */
public record CommentBody(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 2000;

    /**
     * Creates a CommentBody after validating all constraints.
     *
     * @param value the comment text; must not be null, blank, or exceed 2000 characters
     * @throws IllegalArgumentException if any constraint is violated
     */
    public CommentBody {
        if (value == null) {
            throw new IllegalArgumentException("CommentBody value must not be null");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("CommentBody value must not be blank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "CommentBody must be between " + MIN_LENGTH + " and " + MAX_LENGTH
                    + " characters, was: " + value.length());
        }
    }
}
