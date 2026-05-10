package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.CommentId;

/**
 * Thrown when a use case attempts to load a Comment aggregate by id
 * and the repository returns empty.
 *
 * <p>This exception is unchecked because not-found from a repository
 * is typically a programmer error or an invalid client request — the
 * interface layer (REST adapter, added in Bundle 2) translates it to a
 * 404 Not Found response.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 */
public class CommentNotFound extends RuntimeException {

    private final CommentId commentId;

    /**
     * Creates a CommentNotFound exception.
     *
     * @param commentId the id that was not found
     */
    public CommentNotFound(CommentId commentId) {
        super("Comment " + commentId.value() + " not found");
        this.commentId = commentId;
    }

    /**
     * @return the id that was not found
     */
    public CommentId commentId() {
        return commentId;
    }
}
