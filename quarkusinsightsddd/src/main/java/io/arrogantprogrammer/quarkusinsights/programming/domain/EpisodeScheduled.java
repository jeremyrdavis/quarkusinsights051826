package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

/**
 * A domain event recording that an Episode aggregate has been
 * successfully scheduled.
 *
 * <p>Emitted by {@link Episode#schedule}. The Public Catalog
 * subscribes to this event to upsert a denormalized
 * {@code PublicEpisodeView} row in the catalog read model.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param episodeId    the freshly minted ID of the scheduled episode
 * @param number       the episode's sequential number
 * @param airDate      the date on which the episode is to air
 * @param occurredAt   the instant the schedule was recorded; for
 *                     ordering and auditing only
 */
public record EpisodeScheduled(
    EpisodeId episodeId,
    EpisodeNumber number,
    AirDate airDate,
    Instant occurredAt
) implements DomainEvent {
}
