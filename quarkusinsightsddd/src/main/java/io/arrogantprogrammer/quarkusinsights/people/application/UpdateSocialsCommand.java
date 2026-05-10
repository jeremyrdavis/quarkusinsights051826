package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.people.domain.SocialLinks;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Command to update the social links of an existing Person aggregate.
 *
 * <p>Part of the People bounded context, application layer.
 *
 * @param personId the identifier of the Person to update; must not be null
 * @param socials  the new social links; must not be null
 */
public record UpdateSocialsCommand(PersonId personId, SocialLinks socials) {

    /**
     * Creates an UpdateSocialsCommand.
     *
     * @param personId the person's identifier
     * @param socials  the new social links
     * @throws IllegalArgumentException if any parameter is null
     */
    public UpdateSocialsCommand {
        if (personId == null) {
            throw new IllegalArgumentException("UpdateSocialsCommand.personId must not be null");
        }
        if (socials == null) {
            throw new IllegalArgumentException("UpdateSocialsCommand.socials must not be null");
        }
    }
}
