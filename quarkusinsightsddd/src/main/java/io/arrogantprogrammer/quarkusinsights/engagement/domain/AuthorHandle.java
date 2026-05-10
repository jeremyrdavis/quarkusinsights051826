package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import java.util.regex.Pattern;

/**
 * Value Object representing an audience member's handle (username) for
 * engagement actions such as submitting comments and ratings.
 *
 * <p>An AuthorHandle must satisfy all of the following constraints:
 * <ul>
 *   <li>non-null</li>
 *   <li>length between 2 and 40 characters inclusive</li>
 *   <li>only alphanumeric characters, underscores, and hyphens
 *       ({@code [a-zA-Z0-9_-]+})</li>
 * </ul>
 *
 * <p>Validation is enforced in the canonical constructor; violations
 * throw {@link IllegalArgumentException} with a descriptive message.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 *
 * @param value the handle string; must satisfy the constraints above
 */
public record AuthorHandle(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 40;

    /**
     * Creates an AuthorHandle after validating all constraints.
     *
     * @param value the handle string; must not be null, length 2..40,
     *              matching {@code [a-zA-Z0-9_-]+}
     * @throws IllegalArgumentException if any constraint is violated
     */
    public AuthorHandle {
        if (value == null) {
            throw new IllegalArgumentException("AuthorHandle value must not be null");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "AuthorHandle must be between " + MIN_LENGTH + " and " + MAX_LENGTH
                    + " characters, was: " + value.length());
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "AuthorHandle must match [a-zA-Z0-9_-]+, was: " + value);
        }
    }
}
