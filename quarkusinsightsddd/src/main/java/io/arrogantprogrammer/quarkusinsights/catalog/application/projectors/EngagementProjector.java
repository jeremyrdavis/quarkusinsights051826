package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentSubmitted;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingSubmitted;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event projector that keeps comment counts and rating running totals in
 * {@code public_episode_view} up to date with events from the Engagement
 * bounded context.
 *
 * <p>Part of the Public Catalog bounded context, application layer.
 *
 * <p>Subscribed events and their projector actions:
 * <ul>
 *   <li>{@link CommentSubmitted} — finds the view row for the referenced
 *       episode and increments {@code commentCount} by 1. If no view row
 *       exists (the {@code EpisodeScheduled} event has not yet been processed
 *       — possible under concurrent load), the update is a no-op.</li>
 *   <li>{@link RatingSubmitted} — finds the view row and adds the new star
 *       value to the running {@code ratingSum} total, then increments
 *       {@code ratingCount}. The average is computed on demand by
 *       {@link PublicEpisodeViewEntity#averageRating()} rather than stored
 *       directly, so this update is O(1) regardless of the total number of
 *       ratings.</li>
 * </ul>
 *
 * <p>Running-totals design rationale:
 * Storing {@code (ratingSum, ratingCount)} instead of the average directly
 * means the projector never needs to read all past ratings to recompute the
 * mean — each new rating update is a single addition and increment. The only
 * trade-off is that the running sum can drift from exact if ratings are ever
 * deleted (not a supported operation in this demo).
 */
@ApplicationScoped
public class EngagementProjector {

    /**
     * Handles {@link CommentSubmitted}: increments the comment count on the
     * corresponding episode view row.
     *
     * @param event the comment-submission event; never null (CDI guarantees this)
     */
    @Transactional
    public void onCommentSubmitted(@Observes CommentSubmitted event) {
        UUID episodeId = event.episodeId().value();
        PublicEpisodeViewEntity entity = PublicEpisodeViewEntity.findById(episodeId);
        if (entity == null) return; // view not yet projected; safe to skip

        entity.commentCount++;
        entity.lastUpdatedAt = event.occurredAt();
    }

    /**
     * Handles {@link RatingSubmitted}: updates the running rating totals on the
     * corresponding episode view row.
     *
     * <p>The new star value is added to {@code ratingSum} and {@code ratingCount}
     * is incremented by 1. The average is then recomputable as
     * {@code ratingSum / ratingCount} without scanning the Ratings table.
     *
     * @param event the rating-submission event; never null (CDI guarantees this)
     */
    @Transactional
    public void onRatingSubmitted(@Observes RatingSubmitted event) {
        UUID episodeId = event.episodeId().value();
        PublicEpisodeViewEntity entity = PublicEpisodeViewEntity.findById(episodeId);
        if (entity == null) return;

        BigDecimal starValue = BigDecimal.valueOf(event.stars().value());
        entity.ratingSum = entity.ratingSum == null
            ? starValue
            : entity.ratingSum.add(starValue);
        entity.ratingCount++;
        entity.lastUpdatedAt = event.occurredAt();
    }
}
