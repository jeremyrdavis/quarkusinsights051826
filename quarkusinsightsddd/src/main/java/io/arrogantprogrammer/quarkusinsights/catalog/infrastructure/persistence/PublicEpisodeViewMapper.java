package io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.catalog.domain.PresenterSummary;
import io.arrogantprogrammer.quarkusinsights.catalog.domain.PublicEpisodeView;
import io.arrogantprogrammer.quarkusinsights.catalog.domain.SpeakerSummary;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Translates between the {@link PublicEpisodeViewEntity} JPA model and the
 * {@link PublicEpisodeView} read-model record.
 *
 * <p>Part of the Public Catalog bounded context, infrastructure layer.
 *
 * <p>The catalog is strictly read-only from the outside (writes are performed
 * directly by projectors on the entity). No inverse mapping ({@code toEntity})
 * is provided because projectors mutate the entity in place rather than working
 * through the domain record.
 *
 * <p>The mapper is stateless and thread-safe; it can safely be shared across
 * all injection points.
 */
@ApplicationScoped
public class PublicEpisodeViewMapper {

    /**
     * Converts a loaded {@link PublicEpisodeViewEntity} to a
     * {@link PublicEpisodeView} record.
     *
     * <p>Null {@code abstractText} becomes {@code Optional.empty()}.
     * Null {@code averageRating()} result becomes {@code Optional.empty()}.
     * Presenter and speaker embeddables are projected to their domain
     * summary counterparts in stable list order (sorted by personId UUID
     * string for deterministic test assertions).
     *
     * @param entity the loaded entity; must not be null
     * @return the assembled read-model record; never null
     */
    public PublicEpisodeView toView(PublicEpisodeViewEntity entity) {
        Optional<AbstractText> abstract_;
        if (entity.abstractText == null || entity.abstractText.length() < 100) {
            // null or sentinel placeholder (e.g. "[abstract submitted — text pending enrichment]")
            // stored by EpisodeProjector when the AbstractSubmitted event does not carry text
            abstract_ = Optional.empty();
        } else {
            abstract_ = Optional.of(new AbstractText(entity.abstractText));
        }

        List<PresenterSummary> presenters = entity.presenters.stream()
            .sorted((a, b) -> a.personId.toString().compareTo(b.personId.toString()))
            .map(e -> new PresenterSummary(new PersonId(e.personId), e.displayName))
            .toList();

        List<SpeakerSummary> speakers = entity.speakers.stream()
            .sorted((a, b) -> a.personId.toString().compareTo(b.personId.toString()))
            .map(e -> new SpeakerSummary(new PersonId(e.personId), e.displayName))
            .toList();

        return new PublicEpisodeView(
            new EpisodeId(entity.id),
            new EpisodeNumber(entity.number),
            new EpisodeTitle(entity.title),
            new AirDate(entity.airDate),
            entity.status,
            abstract_,
            presenters,
            speakers,
            entity.commentCount,
            Optional.ofNullable(entity.averageRating()),
            entity.ratingCount,
            entity.lastUpdatedAt
        );
    }
}
