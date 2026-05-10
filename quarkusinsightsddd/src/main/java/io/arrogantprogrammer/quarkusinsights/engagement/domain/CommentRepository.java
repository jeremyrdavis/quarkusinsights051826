package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Comment} aggregate root.
 *
 * <p>Defined in the domain layer so application services depend on the
 * port (not on Panache or any other adapter). Implementations live in
 * {@code engagement/infrastructure/persistence/} and are injected via
 * CDI; the domain layer must not import any concrete implementation.
 *
 * <p><strong>Save semantics.</strong> {@link #save(Comment)} is
 * idempotent in identity: calling save twice with the same Comment
 * persists the latest state of that aggregate (the implementation
 * resolves whether to insert or update by id). Save MUST NOT publish
 * domain events; that responsibility belongs to the application
 * service after the save commits.
 *
 * <p><strong>Load semantics.</strong> {@link #findById} returns
 * {@link Optional#empty()} when the aggregate is not found rather than
 * throwing — the absence of a Comment is a valid query result, not an
 * exception.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 */
public interface CommentRepository {

    /**
     * Loads a Comment aggregate by id.
     *
     * @param id the aggregate identifier; must not be null
     * @return the Comment, or {@link Optional#empty()} if no Comment
     *     with that id exists
     */
    Optional<Comment> findById(CommentId id);

    /**
     * Returns all Comments submitted on the given episode, in chronological
     * order by {@code submittedAt} ascending.
     *
     * @param episodeId the episode id to filter by; must not be null
     * @return a list of matching Comments ordered by submission time (possibly empty)
     */
    List<Comment> findByEpisode(EpisodeId episodeId);

    /**
     * Persists a Comment aggregate (insert or update by identity).
     *
     * <p>Implementations MUST NOT publish domain events. The
     * application service is responsible for reading
     * {@link Comment#recordedEvents()} after a successful save and
     * dispatching them, then calling {@link Comment#clearRecordedEvents()}.
     *
     * @param comment the aggregate to persist; must not be null
     */
    void save(Comment comment);
}
