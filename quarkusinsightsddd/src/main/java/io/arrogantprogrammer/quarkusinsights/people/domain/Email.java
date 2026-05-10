package io.arrogantprogrammer.quarkusinsights.people.domain;

/**
 * Email address of a Person — an immutable Value Object.
 *
 * <p>Validation is intentionally pragmatic rather than RFC-5321-perfect:
 * the address must be non-null, contain exactly one {@code @} character,
 * have a non-empty local part (before the {@code @}), have a non-empty
 * domain part (after the {@code @}), and contain no whitespace. This
 * covers the vast majority of valid real-world addresses and rejects
 * obviously malformed input without incurring the complexity of a full
 * RFC regex.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param value the email address; must be non-null, contain exactly one
 *              {@code @}, have non-empty local and domain parts, and
 *              contain no whitespace characters
 */
public record Email(String value) {

    /**
     * Creates an Email.
     *
     * @param value the email address string
     * @throws IllegalArgumentException if {@code value} is null, contains
     *     no {@code @}, contains more than one {@code @}, has an empty
     *     local or domain part, or contains whitespace
     */
    public Email {
        if (value == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Email must not contain whitespace: '" + value + "'");
        }
        long atCount = value.chars().filter(c -> c == '@').count();
        if (atCount == 0) {
            throw new IllegalArgumentException("Email must contain '@': '" + value + "'");
        }
        if (atCount > 1) {
            throw new IllegalArgumentException("Email must contain exactly one '@': '" + value + "'");
        }
        int atIndex = value.indexOf('@');
        if (atIndex == 0) {
            throw new IllegalArgumentException("Email local part must not be empty: '" + value + "'");
        }
        if (atIndex == value.length() - 1) {
            throw new IllegalArgumentException("Email domain part must not be empty: '" + value + "'");
        }
    }
}
