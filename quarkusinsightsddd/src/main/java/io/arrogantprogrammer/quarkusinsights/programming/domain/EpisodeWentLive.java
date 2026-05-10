package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

/**
 * A domain event recording that an Episode has transitioned from
 * {@link EpisodeStatus#SCHEDULED} to {@link EpisodeStatus#LIVE}.
 *
 * <p>Emitted by {@link Episode#goLive()}. The Public Catalog updates
 * the corresponding view's status column in response.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param episodeId  the ID of the episode that went live
 * @param occurredAt the instant the transition was recorded; for
 *                   ordering and auditing only
 */
public record EpisodeWentLive(EpisodeId episodeId, Instant occurredAt) implements DomainEvent {
}
