package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

/**
 * A domain event recording that an episode's abstract has been
 * submitted (or replaced — submitting again before going live
 * supersedes the previous abstract).
 *
 * <p>Emitted by {@link Episode#submitAbstract}. The Public Catalog
 * caches the abstract text in its denormalized view in response.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param episodeId  the ID of the episode that received the abstract
 * @param abstractId the freshly minted ID of the submitted Abstract
 *                   inside-aggregate entity
 * @param occurredAt the instant the abstract was recorded; for
 *                   ordering and auditing only
 */
public record AbstractSubmitted(
    EpisodeId episodeId,
    AbstractId abstractId,
    Instant occurredAt
) implements DomainEvent {
}
