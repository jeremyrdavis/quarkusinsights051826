package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

/**
 * A domain event recording that a Comment aggregate has been submitted
 * by an audience member on a published episode.
 *
 * <p>Emitted by {@link Comment#submit}. The Public Catalog context may
 * subscribe to this event to update its denormalized comment counts.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 *
 * @param commentId  the freshly minted ID of the submitted comment
 * @param episodeId  the ID of the episode on which the comment was submitted
 * @param author     the handle of the audience member who submitted the comment
 * @param occurredAt the instant the submission was recorded; for ordering and
 *                   auditing only
 */
public record CommentSubmitted(
    CommentId commentId,
    EpisodeId episodeId,
    AuthorHandle author,
    Instant occurredAt
) implements DomainEvent {
}
