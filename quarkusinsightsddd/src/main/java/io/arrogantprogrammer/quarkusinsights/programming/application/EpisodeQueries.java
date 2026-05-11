package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.Optional;

/**
 * Read-only cross-context port for fetching {@link EpisodeSummary}
 * projections of Episode aggregates.
 *
 * <p>Mirrors the pattern established by
 * {@code people.application.PersonQueries}: a thin read port the
 * Public Catalog's projectors use to enrich their denormalized views
 * with data that domain events do not carry (specifically, the title
 * and the abstract text — the {@code EpisodeScheduled} and
 * {@code AbstractSubmitted} events carry only IDs).
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
}
