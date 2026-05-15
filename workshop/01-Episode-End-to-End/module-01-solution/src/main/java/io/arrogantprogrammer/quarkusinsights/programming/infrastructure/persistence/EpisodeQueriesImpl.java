package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeSummary;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.enterprise.context.ApplicationScoped;

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
}
