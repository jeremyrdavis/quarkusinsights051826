package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Translates {@link Comment} aggregates into wire-friendly
 * {@link CommentResponse}s for HTTP responses. Unwraps domain VOs
 * so no domain types leak across the JSON boundary.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 */
@ApplicationScoped
public class CommentResponseMapper {

    /**
     * Builds a flat {@link CommentResponse} from a Comment.
     *
     * @param comment the source aggregate; must not be null
     * @return the wire-shaped response
     */
    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
            comment.id().value(),
            comment.episodeId().value(),
            comment.author().value(),
            comment.body().value(),
            comment.submittedAt()
        );
    }
}
