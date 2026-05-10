package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;

/**
 * Domain event recorded when a Person's name is changed via
 * {@link Person#rename}.
 *
 * <p>Carries the new name so subscribers can update denormalized read
 * models without loading the full Person aggregate.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param personId   the identifier of the renamed Person
 * @param newName    the person's name after the rename
 * @param occurredAt the instant the rename was recorded
 */
public record PersonRenamed(
    PersonId personId,
    PersonName newName,
    Instant occurredAt
) implements DomainEvent {
}
