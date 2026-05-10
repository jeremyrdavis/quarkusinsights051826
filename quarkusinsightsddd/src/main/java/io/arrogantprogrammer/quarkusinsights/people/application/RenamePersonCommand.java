package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Command to rename an existing Person aggregate.
 *
 * <p>Part of the People bounded context, application layer.
 *
 * @param personId the identifier of the Person to rename; must not be null
 * @param name     the new full name; must not be null
 */
public record RenamePersonCommand(PersonId personId, PersonName name) {

    /**
     * Creates a RenamePersonCommand.
     *
     * @param personId the person's identifier
     * @param name     the new name
     * @throws IllegalArgumentException if any parameter is null
     */
    public RenamePersonCommand {
        if (personId == null) {
            throw new IllegalArgumentException("RenamePersonCommand.personId must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("RenamePersonCommand.name must not be null");
        }
    }
}
