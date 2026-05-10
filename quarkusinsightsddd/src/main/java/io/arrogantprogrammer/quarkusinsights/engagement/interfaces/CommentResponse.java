package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import java.time.Instant;
import java.util.UUID;

/**
 * Flat JSON response shape for a Comment aggregate.
 *
 * <p>Designed for direct Jackson serialization — all components are
 * primitives or basic JDK types; no domain types leak across the wire.
 * {@link CommentResponseMapper} produces these from a loaded {@code Comment}
 * aggregate.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 *
 * @param id           the Comment's id
 * @param episodeId    the id of the episode this comment was submitted on
 * @param authorHandle the handle of the audience member who submitted the comment
 * @param body         the comment text
 * @param submittedAt  the instant the comment was originally submitted
 */
public record CommentResponse(
    UUID id,
    UUID episodeId,
    String authorHandle,
    String body,
    Instant submittedAt
) {
}
