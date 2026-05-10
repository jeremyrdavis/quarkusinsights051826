package io.arrogantprogrammer.quarkusinsights.catalog.domain;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Denormalized read model for a single episode in the Public Catalog.
 *
 * <p>Part of the Public Catalog bounded context, domain layer.
 *
 * <p>This record is the central projection artifact of the catalog context.
 * Rather than re-assembling an episode view from multiple normalized tables on
 * every HTTP request, the catalog maintains a pre-joined, pre-aggregated row
 * per episode in {@code public_episode_view}. Three event projectors keep this
 * row up to date:
 * <ul>
 *   <li>{@code EpisodeProjector} — reacts to Programming events (schedule,
 *       abstract, presenter/speaker assignments, lifecycle transitions)</li>
 *   <li>{@code PersonProjector} — reacts to People events (name changes,
 *       which must cascade into any embedded PresenterSummary / SpeakerSummary)</li>
 *   <li>{@code EngagementProjector} — reacts to Engagement events (new comments
 *       increment {@code commentCount}; new ratings update the running totals
 *       {@code ratingSum} and {@code ratingCount} so the average is always
 *       current without scanning every rating row)</li>
 * </ul>
 *
 * <p>Optional and List fields are never null — callers should use
 * {@link Optional#empty()} and {@link java.util.Collections#emptyList()}
 * for absent values. The compact constructor enforces this so that query
 * results are always safe to render without null guards.
 *
 * @param id            the episode's unique identifier; must not be null
 * @param number        the episode's sequential number; must not be null
 * @param title         the episode's display title; must not be null
 * @param airDate       the date on which the episode airs; must not be null
 * @param status        the episode's current lifecycle status; must not be null
 * @param abstract_     an Optional wrapping the episode abstract text; must not be null
 *                      (use {@code Optional.empty()} when no abstract has been submitted)
 * @param presenters    the list of presenter summaries; must not be null
 * @param speakers      the list of speaker summaries; must not be null
 * @param commentCount  the total number of comments submitted for this episode; must be &ge; 0
 * @param averageRating an Optional wrapping the mean star rating; must not be null
 *                      (use {@code Optional.empty()} when no ratings have been submitted)
 * @param ratingCount   the total number of ratings submitted for this episode; must be &ge; 0
 * @param lastUpdatedAt the instant the projection was last mutated; must not be null
 */
public record PublicEpisodeView(
    EpisodeId id,
    EpisodeNumber number,
    EpisodeTitle title,
    AirDate airDate,
    EpisodeStatus status,
    Optional<AbstractText> abstract_,
    List<PresenterSummary> presenters,
    List<SpeakerSummary> speakers,
    int commentCount,
    Optional<BigDecimal> averageRating,
    int ratingCount,
    Instant lastUpdatedAt
) {
    /**
     * Creates a PublicEpisodeView, validating all fields.
     *
     * @throws IllegalArgumentException if any required field is null, or if
     *     Optional/List fields are null (they must be empty wrappers, not null)
     */
    public PublicEpisodeView {
        if (id == null || number == null || title == null || airDate == null || status == null) {
            throw new IllegalArgumentException("Required fields must not be null");
        }
        if (abstract_ == null) {
            throw new IllegalArgumentException("abstract_ Optional must not be null (use Optional.empty)");
        }
        if (presenters == null || speakers == null) {
            throw new IllegalArgumentException("List fields must not be null (use empty list)");
        }
        if (averageRating == null) {
            throw new IllegalArgumentException("averageRating Optional must not be null");
        }
        if (lastUpdatedAt == null) {
            throw new IllegalArgumentException("lastUpdatedAt must not be null");
        }
    }
}
