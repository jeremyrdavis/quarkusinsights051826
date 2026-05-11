package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;

/**
 * Thrown when an audience member attempts to submit a second Rating for
 * an episode they have already rated — violating the "one Rating per
 * (EpisodeId, AuthorHandle)" invariant described in SPEC.md §3.6.
 *
 * <p><strong>Dual-source enforcement (§3.6).</strong> This exception is
 * thrown from two distinct places to achieve belt-and-suspenders
 * protection:
 * <ol>
 *   <li><em>Precheck path</em> — {@link io.arrogantprogrammer.quarkusinsights.engagement.application.RatingService#submit(io.arrogantprogrammer.quarkusinsights.engagement.application.SubmitRatingCommand)} calls
 *       {@code RatingRepository#findByEpisodeAndAuthor} before creating
 *       the aggregate. If a matching Rating already exists, AlreadyRated
 *       is thrown immediately with the existing {@code RatingId} as payload.
 *       This is the fast, deterministic path for serial requests.</li>
 *   <li><em>Constraint-translation path</em> — {@code RatingRepositoryImpl#save}
 *       (added in Bundle 2) catches a database UNIQUE constraint violation on
 *       {@code (episode_id, author_handle)} and re-throws AlreadyRated. This
 *       handles concurrent race conditions where two requests pass the precheck
 *       simultaneously and then race to insert.</li>
 * </ol>
 *
 * <p>Callers can inspect {@link #existingRatingId()} to identify the
 * pre-existing Rating, and {@link #episodeId()} / {@link #author()} to
 * confirm which combination was rejected.
 *
 * <p>The REST adapter (added in Bundle 2) translates this exception to a
 * 409 Conflict response with a descriptive error body referencing the
 * existing rating's id.
 *
 * <p>Part of the Engagement bounded context, domain layer. See SPEC.md §3.6.
 */
public class AlreadyRated extends RuntimeException {

    private final RatingId existingRatingId;
    private final EpisodeId episodeId;
    private final AuthorHandle author;

    /**
     * Creates an AlreadyRated exception.
     *
     * @param existingRatingId the id of the pre-existing Rating that conflicts
     *                         with the attempted submission
     * @param episodeId        the episode that was already rated
     * @param author           the author handle that already submitted a rating
     */
    public AlreadyRated(RatingId existingRatingId, EpisodeId episodeId, AuthorHandle author) {
        super("Author '" + author.value() + "' has already rated episode "
            + episodeId.value() + " (existing rating: " + existingRatingId.value() + ")");
        this.existingRatingId = existingRatingId;
        this.episodeId = episodeId;
        this.author = author;
    }

    /**
     * @return the id of the pre-existing Rating that prevented this submission
     */
    public RatingId existingRatingId() {
        return existingRatingId;
    }

    /**
     * @return the episode that was already rated by this author
     */
    public EpisodeId episodeId() {
        return episodeId;
    }

    /**
     * @return the author handle that already has a rating for this episode
     */
    public AuthorHandle author() {
        return author;
    }
}
