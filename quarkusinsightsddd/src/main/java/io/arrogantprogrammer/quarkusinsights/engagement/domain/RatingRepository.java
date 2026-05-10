package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Rating} aggregate root.
 *
 * <p>Defined in the domain layer so application services depend on the
 * port (not on Panache or any other adapter). Implementations live in
 * {@code engagement/infrastructure/persistence/} and are injected via
 * CDI; the domain layer must not import any concrete implementation.
 *
 * <p><strong>Save semantics.</strong> {@link #save(Rating)} is
 * idempotent in identity: calling save twice with the same Rating
 * persists the latest state of that aggregate. Save MUST NOT publish
 * domain events; that responsibility belongs to the application service.
 * Additionally, the implementation MUST translate a database UNIQUE
 * constraint violation on {@code (episode_id, author_handle)} into an
 * {@link AlreadyRated} exception (the constraint-translation half of
 * SPEC.md §3.6).
 *
 * <p><strong>Precheck support.</strong> {@link #findByEpisodeAndAuthor}
 * exists specifically to support the §3.6 precheck in
 * {@link io.arrogantprogrammer.quarkusinsights.engagement.application.RatingService#submit}.
 * The application service calls this before creating the Rating
 * aggregate to give an early, deterministic rejection for serial
 * duplicate submissions.
 *
 * <p><strong>Load semantics.</strong> {@link #findById} and
 * {@link #findByEpisodeAndAuthor} return {@link Optional#empty()} when
 * no matching aggregate is found rather than throwing.
 *
 * <p>Part of the Engagement bounded context, domain layer. See SPEC.md §3.6.
 */
public interface RatingRepository {

    /**
     * Loads a Rating aggregate by id.
     *
     * @param id the aggregate identifier; must not be null
     * @return the Rating, or {@link Optional#empty()} if no Rating
     *     with that id exists
     */
    Optional<Rating> findById(RatingId id);

    /**
     * Checks whether an author has already rated a specific episode.
     *
     * <p>This method exists specifically for the §3.6 precheck in
     * {@code RatingService.submit}. A non-empty result means the author
     * has already rated this episode, and {@code RatingService} will
     * throw {@link AlreadyRated} before creating a duplicate Rating.
     *
     * @param episodeId the episode id; must not be null
     * @param author    the author handle; must not be null
     * @return the existing Rating, or {@link Optional#empty()} if the
     *     author has not yet rated this episode
     */
    Optional<Rating> findByEpisodeAndAuthor(EpisodeId episodeId, AuthorHandle author);

    /**
     * Returns all Ratings submitted on the given episode, in unspecified order.
     *
     * @param episodeId the episode id to filter by; must not be null
     * @return a list of matching Ratings (possibly empty)
     */
    List<Rating> findByEpisode(EpisodeId episodeId);

    /**
     * Persists a Rating aggregate (insert or update by identity).
     *
     * <p>Implementations MUST NOT publish domain events. The
     * application service is responsible for reading
     * {@link Rating#recordedEvents()} after a successful save and
     * dispatching them, then calling {@link Rating#clearRecordedEvents()}.
     *
     * @param rating the aggregate to persist; must not be null
     * @throws AlreadyRated if the persistence layer detects a UNIQUE
     *     constraint violation on {@code (episode_id, author_handle)},
     *     indicating a concurrent race past the precheck
     */
    void save(Rating rating);
}
