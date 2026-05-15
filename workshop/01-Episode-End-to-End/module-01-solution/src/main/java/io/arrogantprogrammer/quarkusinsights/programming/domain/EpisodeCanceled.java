package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

/**
 * A domain event recording that a scheduled Episode was canceled
 * before going live.
 *
 * <p>Emitted by {@link Episode#cancel(String)}. The Public Catalog
 * removes the episode from upcoming-show displays in response.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param episodeId  the ID of the canceled episode
 * @param reason     a human-readable cancellation reason
 * @param occurredAt the instant the cancellation was recorded; for
 *                   ordering and auditing only
 */
public record EpisodeCanceled(
    EpisodeId episodeId,
    String reason,
    Instant occurredAt
) implements DomainEvent {
}
