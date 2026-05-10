package io.arrogantprogrammer.quarkusinsights.people.domain;

/**
 * Full name of a Person — an immutable Value Object composed of a first
 * and last name.
 *
 * <p>Both components must be non-null, non-blank, and between 1 and 100
 * characters (inclusive). The length bounds are intentionally generous to
 * accommodate a wide variety of cultural naming conventions.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param first the person's given (first) name; must not be null, blank,
 *              or exceed 100 characters
 * @param last  the person's family (last) name; must not be null, blank,
 *              or exceed 100 characters
 */
public record PersonName(String first, String last) {

    private static final int MAX_LENGTH = 100;

    /**
     * Creates a PersonName.
     *
     * @param first the given name
     * @param last  the family name
     * @throws IllegalArgumentException if either component is null, blank,
     *     shorter than 1 character, or longer than 100 characters
     */
    public PersonName {
        if (first == null) {
            throw new IllegalArgumentException("PersonName.first must not be null");
        }
        if (first.isBlank()) {
            throw new IllegalArgumentException("PersonName.first must not be blank");
        }
        if (first.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "PersonName.first must not exceed " + MAX_LENGTH
                + " characters, got: " + first.length());
        }
        if (last == null) {
            throw new IllegalArgumentException("PersonName.last must not be null");
        }
        if (last.isBlank()) {
            throw new IllegalArgumentException("PersonName.last must not be blank");
        }
        if (last.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "PersonName.last must not exceed " + MAX_LENGTH
                + " characters, got: " + last.length());
        }
    }

    /**
     * Returns the person's display name in "first last" format.
     *
     * @return first name, a single space, then last name
     */
    public String displayName() {
        return first + " " + last;
    }
}
