package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.util.List;
import java.util.Optional;

/**
 * Read-only query port for cross-context access to Person summaries.
 *
 * <p>This port is intentionally separate from the write-side
 * {@link io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository}
 * to enforce CQRS discipline: consumers in other bounded contexts depend only
 * on this thin query interface, not on the full aggregate.
 *
 * <p>The implementation ({@code PersonQueriesImpl}) lives in the People
 * context's persistence layer (Bundle 2) and is injected via CDI.
 *
 * <p>Part of the People bounded context, application layer.
 */
public interface PersonQueries {

    /**
     * Read-only lookup of a Person summary by id, intended for
     * cross-context consumers (notably the Catalog projector in Plan 5).
     *
     * @param id the PersonId; must not be null
     * @return a flat summary, or empty if no Person with that id exists
     */
    Optional<PersonSummary> findById(PersonId id);

    /**
     * Returns a page of Person summaries, optionally filtered by a
     * case-insensitive substring match across first and last name.
     * Ordered alphabetically by last name then first name.
     *
     * @param search optional substring; empty returns all
     * @param page   zero-based page index; must be &ge; 0
     * @param size   maximum page size; must be &ge; 1
     * @return matching summaries (possibly empty)
     */
    List<PersonSummary> search(Optional<String> search, int page, int size);

    /**
     * Total count of People, optionally filtered by the same search
     * substring used by {@link #search}.
     *
     * @param search optional substring; empty counts all
     * @return the total count
     */
    long countAll(Optional<String> search);
}
