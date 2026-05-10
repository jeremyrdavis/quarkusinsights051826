package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Flat read-only summary of a Person for cross-context consumers.
 *
 * <p>This record is intentionally thin — it carries only the fields that
 * downstream bounded contexts (notably the Catalog projector in Plan 5)
 * need without exposing the full {@link io.arrogantprogrammer.quarkusinsights.people.domain.Person}
 * aggregate or its Value Objects.
 *
 * <p>Part of the People bounded context, application layer.
 *
 * @param id          the person's unique identifier; must not be null
 * @param displayName the person's display name (first + last); must not be null
 * @param email       the person's email address string; must not be null
 */
public record PersonSummary(PersonId id, String displayName, String email) {

    /**
     * Creates a PersonSummary.
     *
     * @param id          the person's identifier
     * @param displayName the formatted display name
     * @param email       the email address string
     * @throws IllegalArgumentException if any component is null
     */
    public PersonSummary {
        if (id == null) {
            throw new IllegalArgumentException("PersonSummary.id must not be null");
        }
        if (displayName == null) {
            throw new IllegalArgumentException("PersonSummary.displayName must not be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("PersonSummary.email must not be null");
        }
    }
}
