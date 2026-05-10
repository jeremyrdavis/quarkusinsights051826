package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;

/**
 * Command to register a new Person aggregate.
 *
 * <p>Part of the People bounded context, application layer.
 *
 * @param name  the person's full name; must not be null
 * @param email the person's email address; must not be null
 * @param bio   the person's biography; must not be null
 */
public record RegisterPersonCommand(PersonName name, Email email, Bio bio) {

    /**
     * Creates a RegisterPersonCommand.
     *
     * @param name  the full name
     * @param email the email address
     * @param bio   the biography
     * @throws IllegalArgumentException if any parameter is null
     */
    public RegisterPersonCommand {
        if (name == null) {
            throw new IllegalArgumentException("RegisterPersonCommand.name must not be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("RegisterPersonCommand.email must not be null");
        }
        if (bio == null) {
            throw new IllegalArgumentException("RegisterPersonCommand.bio must not be null");
        }
    }
}
