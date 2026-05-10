package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;

import java.time.Instant;

/**
 * A domain event recording that a Rating aggregate has been submitted
 * by an audience member on a published episode.
 *
 * <p>Emitted by {@link Rating#submit}. The Public Catalog context may
 * subscribe to this event to update its denormalized average-rating
 * projection.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 *
 * @param ratingId   the freshly minted ID of the submitted rating
 * @param episodeId  the ID of the episode on which the rating was submitted
 * @param author     the handle of the audience member who submitted the rating
 * @param stars      the star rating value submitted
 * @param occurredAt the instant the submission was recorded; for ordering and
 *                   auditing only
 */
public record RatingSubmitted(
    RatingId ratingId,
    EpisodeId episodeId,
    AuthorHandle author,
    Stars stars,
    Instant occurredAt
) implements DomainEvent {
}
