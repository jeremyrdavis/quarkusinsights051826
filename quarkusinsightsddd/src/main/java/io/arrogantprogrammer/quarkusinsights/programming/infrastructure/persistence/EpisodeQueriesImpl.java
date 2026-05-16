package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeListRow;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeSummary;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of {@link EpisodeQueries}. Reads
 * directly from {@link EpisodeEntity} (the adapter has direct access
 * to the persistence model by design).
 *
 * <p>Part of the Programming bounded context, infrastructure layer.
 */
@ApplicationScoped
public class EpisodeQueriesImpl implements EpisodeQueries {

    @Override
    public Optional<EpisodeSummary> findById(EpisodeId id) {
        EpisodeEntity entity = EpisodeEntity.findById(id.value());
        if (entity == null) {
            return Optional.empty();
        }
        Optional<AbstractText> abstractText = Optional.empty();
        if (entity.theAbstract != null && entity.theAbstract.abstractText != null) {
            abstractText = Optional.of(new AbstractText(entity.theAbstract.abstractText));
        }
        return Optional.of(new EpisodeSummary(
            new EpisodeId(entity.id),
            new EpisodeNumber(entity.number),
            new EpisodeTitle(entity.title),
            new AirDate(entity.airDate),
            entity.status,
            abstractText
        ));
    }

    @Override
    public List<EpisodeListRow> listAll(int page, int size, Optional<EpisodeStatus> status) {
        Sort byNumber = Sort.by("number");
        List<EpisodeEntity> entities = status
            .map(s -> EpisodeEntity.<EpisodeEntity>find("status", byNumber, s).page(page, size).list())
            .orElseGet(() -> EpisodeEntity.<EpisodeEntity>findAll(byNumber).page(page, size).list());
        return entities.stream().map(this::toRow).toList();
    }

    @Override
    public long countAll(Optional<EpisodeStatus> status) {
        return status
            .map(s -> EpisodeEntity.count("status", s))
            .orElseGet(() -> EpisodeEntity.count());
    }

    private EpisodeListRow toRow(EpisodeEntity e) {
        boolean hasAbstract = e.theAbstract != null && e.theAbstract.abstractId != null;
        return new EpisodeListRow(
            new EpisodeId(e.id),
            e.number,
            e.title,
            e.airDate,
            e.status,
            e.presenters == null ? 0 : e.presenters.size(),
            e.speakers == null ? 0 : e.speakers.size(),
            hasAbstract
        );
    }
}
