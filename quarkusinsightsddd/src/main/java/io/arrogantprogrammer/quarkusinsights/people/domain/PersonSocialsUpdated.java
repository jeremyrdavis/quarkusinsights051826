package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;

/**
 * Domain event recorded when a Person's social links are changed via
 * {@link Person#updateSocials}.
 *
 * <p>This event is a signal only — it does not carry the new social link
 * values. Subscribers that need the updated links must load the Person
 * or its query model.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param personId   the identifier of the Person whose social links changed
 * @param occurredAt the instant the social link update was recorded
 */
public record PersonSocialsUpdated(
    PersonId personId,
    Instant occurredAt
) implements DomainEvent {
}
