package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.Objects;

/**
 * Command to submit a new star Rating on a published episode.
 *
 * <p>All fields are required; the canonical constructor validates
 * non-null preconditions.
 *
 * <p>Part of the Engagement bounded context, application layer.
 *
 * @param episodeId the id of the episode being rated; must not be null
 * @param author    the handle of the rater; must not be null
 * @param stars     the star rating (1–5); must not be null
 */
public record SubmitRatingCommand(EpisodeId episodeId, AuthorHandle author, Stars stars) {

    /**
     * Creates a SubmitRatingCommand, validating that all fields are non-null.
     *
     * @throws NullPointerException if any field is null
     */
    public SubmitRatingCommand {
        Objects.requireNonNull(episodeId, "episodeId must not be null");
        Objects.requireNonNull(author, "author must not be null");
        Objects.requireNonNull(stars, "stars must not be null");
    }
}
