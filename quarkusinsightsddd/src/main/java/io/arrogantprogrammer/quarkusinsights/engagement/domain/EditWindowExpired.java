package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.CommentId;

import java.time.Instant;

/**
 * Thrown when a caller attempts to edit a Comment aggregate but the
 * 5-minute edit window has elapsed since the comment was submitted.
 *
 * <p><strong>Policy.</strong> The Engagement bounded context allows an
 * author to correct a comment for up to 5 minutes after submission.
 * After that window, the comment is considered immutable — attempts to
 * edit it are rejected by {@link Comment#edit(CommentBody)} with this
 * exception. The window is measured as the {@link java.time.Duration}
 * between {@link Comment#submittedAt()} and {@link Instant#now()} at
 * the time of the edit attempt.
 *
 * <p>The REST adapter (added in Bundle 2) translates this exception to
 * a 409 Conflict response with a descriptive error body.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 */
public class EditWindowExpired extends RuntimeException {

    private final CommentId commentId;
    private final Instant submittedAt;

    /**
     * Creates an EditWindowExpired exception.
     *
     * @param commentId   the id of the comment whose edit window has expired
     * @param submittedAt the instant the comment was originally submitted,
     *                    retained for error reporting
     */
    public EditWindowExpired(CommentId commentId, Instant submittedAt) {
        super("Edit window expired for Comment " + commentId.value()
            + "; submitted at " + submittedAt + " (5-minute window has closed)");
        this.commentId = commentId;
        this.submittedAt = submittedAt;
    }

    /**
     * @return the id of the comment whose edit window has expired
     */
    public CommentId commentId() {
        return commentId;
    }

    /**
     * @return the instant the comment was originally submitted
     */
    public Instant submittedAt() {
        return submittedAt;
    }
}
