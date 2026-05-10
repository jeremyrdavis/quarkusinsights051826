package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.Objects;

/**
 * Command to submit a new Comment on a published episode.
 *
 * <p>All fields are required; the canonical constructor validates
 * non-null preconditions.
 *
 * <p>Part of the Engagement bounded context, application layer.
 *
 * @param episodeId the id of the episode being commented on; must not be null
 * @param author    the handle of the commenter; must not be null
 * @param body      the comment text; must not be null
 */
public record SubmitCommentCommand(EpisodeId episodeId, AuthorHandle author, CommentBody body) {

    /**
     * Creates a SubmitCommentCommand, validating that all fields are non-null.
     *
     * @throws NullPointerException if any field is null
     */
    public SubmitCommentCommand {
        Objects.requireNonNull(episodeId, "episodeId must not be null");
        Objects.requireNonNull(author, "author must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
