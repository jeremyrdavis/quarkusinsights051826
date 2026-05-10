package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Thrown when a use case attempts to load a Person aggregate by id
 * and {@link PersonRepository#findById} returns empty.
 *
 * <p>This exception is unchecked because not-found from a repository
 * is typically a programmer error or an invalid client request — the
 * application layer translates it (via REST exception mappers) to a
 * 404 Not Found response.
 *
 * <p>Part of the People bounded context, domain layer.
 */
public class PersonNotFound extends RuntimeException {

    private final PersonId personId;

    /**
     * Creates a PersonNotFound exception.
     *
     * @param personId the id that was not found
     */
    public PersonNotFound(PersonId personId) {
        super("Person " + personId.value() + " not found");
        this.personId = personId;
    }

    /**
     * @return the id that was not found
     */
    public PersonId personId() {
        return personId;
    }
}
