package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Episode} aggregate root.
 *
 * <p>Defined in the domain layer so application services depend on the
 * port (not on Panache or any other adapter). Implementations live in
 * {@code programming/infrastructure/persistence/} and are injected via
 * CDI; the domain layer must not import any concrete implementation.
 *
 * <p><strong>Save semantics.</strong> {@link #save(Episode)} is
 * idempotent in identity: calling save twice with the same Episode
 * persists the latest state of that aggregate (the implementation
 * resolves whether to insert or update by id). Save MUST NOT publish
 * domain events; that responsibility belongs to the application
 * service after the save commits.
 *
 * <p><strong>Load semantics.</strong> {@link #findById} and
 * {@link #findByNumber} return {@link Optional#empty()} when the
 * aggregate is not found rather than throwing — the absence of an
 * Episode is a valid query result, not an exception.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public interface EpisodeRepository {

    /**
     * Loads an Episode aggregate by id.
     *
     * @param id the aggregate identifier; must not be null
     * @return the Episode, or {@link Optional#empty()} if no Episode
     *     with that id exists
     */
    Optional<Episode> findById(EpisodeId id);

    /**
     * Loads an Episode aggregate by its sequential number.
     *
     * <p>EpisodeNumber is unique (constraint enforced at the persistence
     * layer); at most one Episode is returned.
     *
     * @param number the episode number; must not be null
     * @return the Episode, or {@link Optional#empty()} if no Episode
     *     with that number exists
     */
    Optional<Episode> findByNumber(EpisodeNumber number);

    /**
     * Returns all Episodes currently in the given status, in
     * unspecified order.
     *
     * <p>Implementations may impose an order (e.g., by air date) — this
     * port does not require one. Callers that need a specific order
     * MUST sort the returned list themselves.
     *
     * @param status the status to filter by; must not be null
     * @return a list of matching Episodes (possibly empty)
     */
    List<Episode> findByStatus(EpisodeStatus status);

    /**
     * Persists an Episode aggregate (insert or update by identity).
     *
     * <p>Implementations MUST NOT publish domain events. The
     * application service is responsible for reading
     * {@link Episode#recordedEvents()} after a successful save and
     * dispatching them, then calling {@link Episode#clearRecordedEvents()}.
     *
     * @param episode the aggregate to persist; must not be null
     */
    void save(Episode episode);
}
