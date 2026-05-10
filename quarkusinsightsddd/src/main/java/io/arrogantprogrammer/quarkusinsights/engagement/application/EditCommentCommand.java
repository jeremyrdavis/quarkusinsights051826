package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;

import java.util.Objects;

/**
 * Command to edit the body of an existing Comment within the 5-minute
 * edit window.
 *
 * <p>All fields are required; the canonical constructor validates
 * non-null preconditions.
 *
 * <p>Part of the Engagement bounded context, application layer.
 *
 * @param commentId the id of the comment to edit; must not be null
 * @param body      the replacement comment body; must not be null
 */
public record EditCommentCommand(CommentId commentId, CommentBody body) {

    /**
     * Creates an EditCommentCommand, validating that all fields are non-null.
     *
     * @throws NullPointerException if any field is null
     */
    public EditCommentCommand {
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
