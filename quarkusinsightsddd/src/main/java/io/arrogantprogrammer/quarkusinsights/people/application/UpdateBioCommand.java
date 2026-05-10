package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Command to update the biography of an existing Person aggregate.
 *
 * <p>Part of the People bounded context, application layer.
 *
 * @param personId the identifier of the Person to update; must not be null
 * @param bio      the new biography; must not be null
 */
public record UpdateBioCommand(PersonId personId, Bio bio) {

    /**
     * Creates an UpdateBioCommand.
     *
     * @param personId the person's identifier
     * @param bio      the new biography
     * @throws IllegalArgumentException if any parameter is null
     */
    public UpdateBioCommand {
        if (personId == null) {
            throw new IllegalArgumentException("UpdateBioCommand.personId must not be null");
        }
        if (bio == null) {
            throw new IllegalArgumentException("UpdateBioCommand.bio must not be null");
        }
    }
}
