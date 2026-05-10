package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;

import java.time.Instant;

/**
 * A domain event recording that the body of an existing Comment aggregate
 * has been edited by its author within the permitted 5-minute edit window.
 *
 * <p>Emitted by {@link Comment#edit(CommentBody)}. This is a signal-only
 * event — the new body content is not included because subscribers that
 * need the latest body should re-fetch the Comment from the repository.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 *
 * @param commentId  the ID of the comment that was edited
 * @param occurredAt the instant the edit was recorded; for ordering and
 *                   auditing only
 */
public record CommentEdited(
    CommentId commentId,
    Instant occurredAt
) implements DomainEvent {
}
