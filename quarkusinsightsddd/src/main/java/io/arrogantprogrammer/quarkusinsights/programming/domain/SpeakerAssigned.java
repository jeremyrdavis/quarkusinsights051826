package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;

/**
 * A domain event recording that a Person has been assigned as a
 * speaker (guest) on an Episode.
 *
 * <p>Emitted by {@link Episode#assignSpeaker(PersonId)}. Idempotent
 * at the aggregate level — assigning the same person twice does not
 * emit a second event. The Public Catalog uses this event to
 * denormalize a speaker summary onto the corresponding view row.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param episodeId  the ID of the episode receiving the assignment
 * @param personId   the ID of the person assigned as speaker
 * @param occurredAt the instant the assignment was recorded; for
 *                   ordering and auditing only
 */
public record SpeakerAssigned(
    EpisodeId episodeId,
    PersonId personId,
    Instant occurredAt
) implements DomainEvent {
}
