package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

/**
 * A domain event recording that an Episode has been published —
 * the terminal state in the normal lifecycle.
 *
 * <p>Emitted by {@link Episode#publish()}. The Public Catalog updates
 * the view's status to {@link EpisodeStatus#PUBLISHED} so the episode
 * appears in the public history list. Other potential subscribers
 * include notification systems (out of scope for this demo).
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param episodeId  the ID of the episode that was published
 * @param number     the episode's sequential number (carried in the
 *                   payload because consumers commonly index on it)
 * @param occurredAt the instant the publication was recorded; for
 *                   ordering and auditing only
 */
public record EpisodePublished(
    EpisodeId episodeId,
    EpisodeNumber number,
    Instant occurredAt
) implements DomainEvent {
}
