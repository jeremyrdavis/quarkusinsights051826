package io.arrogantprogrammer.quarkusinsights.catalog.application;

import io.arrogantprogrammer.quarkusinsights.catalog.domain.PublicEpisodeView;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;

import java.util.List;
import java.util.Optional;

/**
 * Read-only query port for the Public Catalog bounded context.
 *
 * <p>Part of the Public Catalog bounded context, application layer.
 *
 * <p>This interface defines the three queries exposed by the catalog read side.
 * Implementations live in the infrastructure layer
 * ({@code catalog.infrastructure.persistence.PublicCatalogQueriesImpl}) and
 * are injected via CDI. The interface keeps the application layer free of any
 * persistence technology references.
 *
 * <p>All methods return views from the pre-built {@code public_episode_view}
 * table rather than assembling episode data at query time. Results reflect the
 * state of the projection at the time of the call; eventual consistency with
 * the write-side aggregates is expected and acceptable for read-only catalog
 * display.
 */
public interface PublicCatalogQueries {

    /**
     * Returns the next upcoming episode: the earliest-air-date SCHEDULED
     * episode, if any.
     *
     * <p>Used to render the "next show" widget on the home page. Returns
     * {@code Optional.empty()} when no episodes are currently scheduled (e.g.,
     * in a hiatus between seasons).
     *
     * @return the upcoming episode view, or empty if none is scheduled
     */
    Optional<PublicEpisodeView> findUpcoming();

    /**
     * Returns a page of PUBLISHED episodes in reverse-chronological order
     * (most recently published first).
     *
     * <p>Used to render the episode history / back-catalog list. Callers should
     * pass {@code page=0, size=20} for the first page and increment {@code page}
     * for subsequent pages. An empty list is returned when the requested page
     * falls beyond the end of the result set.
     *
     * @param page zero-based page index; must be &ge; 0
     * @param size maximum number of results per page; must be &gt; 0
     * @return a list of published episode views, ordered newest-first; never null
     */
    List<PublicEpisodeView> findHistory(int page, int size);

    /**
     * Returns the episode view for the given episode number, regardless of
     * status.
     *
     * <p>Used for the episode detail page and for direct links shared on social
     * media. Returns {@code Optional.empty()} if no episode with the given
     * number has been projected yet (e.g., because the {@code EpisodeScheduled}
     * event has not yet been processed).
     *
     * @param number the episode's sequential number; must not be null
     * @return the episode view, or empty if not found
     */
    Optional<PublicEpisodeView> findByNumber(EpisodeNumber number);
}
