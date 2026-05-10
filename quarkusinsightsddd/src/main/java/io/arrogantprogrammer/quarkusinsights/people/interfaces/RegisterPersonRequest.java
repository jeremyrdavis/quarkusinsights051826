package io.arrogantprogrammer.quarkusinsights.people.interfaces;

/**
 * Request body for {@code POST /people}.
 *
 * <p>JSON shape:
 * <pre>{@code
 * {
 *   "name": {"first": "Holly", "last": "Cummins"},
 *   "email": "holly@example.com",
 *   "bio": "..."
 * }
 * }</pre>
 *
 * <p>Part of the People bounded context, interfaces layer.
 *
 * @param name  the person's name (nested object)
 * @param email the person's email address
 * @param bio   the person's biography
 */
public record RegisterPersonRequest(NameDto name, String email, String bio) {
}
