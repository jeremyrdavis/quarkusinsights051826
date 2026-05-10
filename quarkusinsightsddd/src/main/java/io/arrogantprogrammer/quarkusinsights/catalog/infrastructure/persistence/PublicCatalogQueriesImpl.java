package io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.catalog.application.PublicCatalogQueries;
import io.arrogantprogrammer.quarkusinsights.catalog.domain.PublicEpisodeView;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of {@link PublicCatalogQueries}.
 *
 * <p>Part of the Public Catalog bounded context, infrastructure layer.
 *
 * <p>All queries target the {@link PublicEpisodeViewEntity} table, which is
 * maintained by the three event projectors in
 * {@code catalog.application.projectors}. Results are mapped to the
 * {@link io.arrogantprogrammer.quarkusinsights.catalog.domain.PublicEpisodeView}
 * read-model record via {@link PublicEpisodeViewMapper}.
 *
 * <p>No write operations exist here — the catalog is strictly read-only from
 * the query side. Projectors update the entity table directly within their
 * own {@code @Transactional} observer methods.
 */
@ApplicationScoped
public class PublicCatalogQueriesImpl implements PublicCatalogQueries {

    private final PublicEpisodeViewMapper mapper;

    /**
     * Creates the query adapter.
     *
     * @param mapper the entity-to-record mapper; injected by CDI
     */
    @Inject
    public PublicCatalogQueriesImpl(PublicEpisodeViewMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the SCHEDULED episode with the earliest air date. If multiple
     * episodes share the same earliest air date (unlikely in practice), the
     * Panache {@code firstResultOptional} returns whichever the database returns
     * first — tie-breaking is not specified.
     */
    @Override
    public Optional<PublicEpisodeView> findUpcoming() {
        return PublicEpisodeViewEntity
            .find("status = ?1 ORDER BY airDate ASC", EpisodeStatus.SCHEDULED)
            .firstResultOptional()
            .map(e -> mapper.toView((PublicEpisodeViewEntity) e));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries only PUBLISHED episodes, ordered by air date descending so
     * that the most recently broadcast episode appears first. Panache's
     * {@code page(page, size)} applies SQL LIMIT/OFFSET.
     */
    @Override
    public List<PublicEpisodeView> findHistory(int page, int size) {
        return PublicEpisodeViewEntity
            .<PublicEpisodeViewEntity>find("status = ?1 ORDER BY airDate DESC", EpisodeStatus.PUBLISHED)
            .page(page, size)
            .list()
            .stream()
            .map(mapper::toView)
            .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Looks up the episode by its sequential {@code number} field regardless
     * of status. Returns empty if the projection has not yet received the
     * {@code EpisodeScheduled} event for this number.
     */
    @Override
    public Optional<PublicEpisodeView> findByNumber(EpisodeNumber number) {
        return PublicEpisodeViewEntity
            .<PublicEpisodeViewEntity>find("number", number.value())
            .firstResultOptional()
            .map(mapper::toView);
    }
}
