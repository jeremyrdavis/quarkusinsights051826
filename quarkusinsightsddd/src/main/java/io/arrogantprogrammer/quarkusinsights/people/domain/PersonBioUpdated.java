package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;

/**
 * Domain event recorded when a Person's biography is changed via
 * {@link Person#updateBio}.
 *
 * <p>This event is a signal only — it does not carry the new bio content.
 * Subscribers that need the bio text must load the Person (or subscribe
 * to a richer query model). This intentional omission keeps the event
 * payload small and avoids duplicating potentially large text in the
 * event log.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param personId   the identifier of the Person whose bio changed
 * @param occurredAt the instant the bio update was recorded
 */
public record PersonBioUpdated(
    PersonId personId,
    Instant occurredAt
) implements DomainEvent {
}
