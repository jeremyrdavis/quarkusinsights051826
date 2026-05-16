package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.List;
import java.util.Optional;

/**
 * Read-only port for fetching projections of Episode aggregates.
 *
 * <p>Mirrors the pattern established by
 * {@code people.application.PersonQueries}: a thin read port consumed
 * by cross-context projectors and by driving adapters (notably the
 * admin UI) that need read-side data without rehydrating full
 * aggregates.
 *
 * <p>Implemented by {@code programming.infrastructure.persistence.EpisodeQueriesImpl}.
 *
 * <p>Part of the Programming bounded context, application layer.
 */
public interface EpisodeQueries {

    /**
     * Read-only lookup of an Episode summary by id, intended for
     * cross-context consumers.
     *
     * @param id the EpisodeId; must not be null
     * @return a flat summary, or empty if no Episode with that id exists
     */
    Optional<EpisodeSummary> findById(EpisodeId id);

    /**
     * Returns a page of Episode rows for the admin list view, ordered
     * by sequential episode number (ascending). Filters by status when
     * one is supplied.
     *
     * @param page   zero-based page index; must be &ge; 0
     * @param size   maximum page size; must be &ge; 1
     * @param status optional status filter; empty returns all statuses
     * @return the matching rows (possibly empty)
     */
    List<EpisodeListRow> listAll(int page, int size, Optional<EpisodeStatus> status);

    /**
     * Total count of Episodes, optionally filtered by status, used to
     * compute pagination controls.
     *
     * @param status optional status filter; empty counts all statuses
     * @return the total count
     */
    long countAll(Optional<EpisodeStatus> status);
}
