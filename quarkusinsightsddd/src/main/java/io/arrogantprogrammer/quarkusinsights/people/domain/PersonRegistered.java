package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;

/**
 * Domain event recorded when a new Person aggregate has been successfully
 * registered via {@link Person#register}.
 *
 * <p>Carries the person's initial name and email so downstream subscribers
 * (notably the Catalog projector in Plan 5) can populate their read models
 * without a secondary lookup.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param personId   the freshly minted identifier of the registered Person
 * @param name       the person's initial name
 * @param email      the person's email address at registration time
 * @param occurredAt the instant the registration was recorded
 */
public record PersonRegistered(
    PersonId personId,
    PersonName name,
    Email email,
    Instant occurredAt
) implements DomainEvent {
}
