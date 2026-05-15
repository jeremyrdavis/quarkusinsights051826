package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Abstract;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractId;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Translates between the {@link Episode} domain aggregate and the
 * {@link EpisodeEntity} JPA persistence model.
 *
 * <p>The mapper is the single bridge between the two worlds: the
 * domain knows nothing about JPA, and the entity knows nothing about
 * domain methods. Construction direction is asymmetric:
 * <ul>
 *   <li>{@link #toEntity}: produces a fresh {@code EpisodeEntity}
 *       from an Episode, suitable for {@code persist()}.</li>
 *   <li>{@link #toDomain}: produces a fresh {@link Episode} from a
 *       loaded entity using
 *       {@link Episode#rehydrate} (does not record any domain
 *       events).</li>
 *   <li>{@link #applyTo}: copies an Episode's mutable state onto an
 *       already-managed entity, preserving JPA-managed fields
 *       (notably {@code @Version}). Used on update.</li>
 * </ul>
 *
 * <p>Part of the Programming bounded context, infrastructure layer.
 */
@ApplicationScoped
public class EpisodeMapper {

    /**
     * Builds a fresh {@link EpisodeEntity} mirroring the given
     * {@link Episode}. Used when persisting a new aggregate.
     *
     * @param episode the source aggregate; must not be null
     * @return a fresh entity with all fields populated
     */
    public EpisodeEntity toEntity(Episode episode) {
        EpisodeEntity entity = new EpisodeEntity();
        entity.id = episode.id().value();
        entity.number = episode.number().value();
        entity.title = episode.title().value();
        entity.airDate = episode.airDate().value();
        if (episode.theAbstract() != null) {
            entity.theAbstract = embeddableFor(episode.theAbstract());
        }
        entity.presenters = episode.presenters().stream()
            .map(PersonId::value)
            .collect(Collectors.toCollection(HashSet::new));
        entity.speakers = episode.speakers().stream()
            .map(PersonId::value)
            .collect(Collectors.toCollection(HashSet::new));
        entity.status = episode.status();
        entity.createdAt = episode.createdAt();
        entity.updatedAt = episode.updatedAt();
        return entity;
    }

    /**
     * Reconstructs an {@link Episode} aggregate from its persisted
     * state. Uses {@link Episode#rehydrate} so no domain events are
     * recorded — load is not a domain action.
     *
     * @param entity the loaded entity; must not be null
     * @return the rehydrated Episode
     */
    public Episode toDomain(EpisodeEntity entity) {
        Abstract theAbstract = null;
        if (entity.theAbstract != null && entity.theAbstract.abstractId != null) {
            theAbstract = new Abstract(
                new AbstractId(entity.theAbstract.abstractId),
                new AbstractText(entity.theAbstract.abstractText),
                entity.theAbstract.abstractSubmittedAt
            );
        }
        return Episode.rehydrate(
            new EpisodeId(entity.id),
            new EpisodeNumber(entity.number),
            new EpisodeTitle(entity.title),
            new AirDate(entity.airDate),
            theAbstract,
            entity.presenters.stream().map(PersonId::new).collect(Collectors.toSet()),
            entity.speakers.stream().map(PersonId::new).collect(Collectors.toSet()),
            entity.status,
            entity.createdAt,
            entity.updatedAt
        );
    }

    /**
     * Copies the mutable state of an {@link Episode} onto an
     * already-managed {@link EpisodeEntity}, preserving the
     * entity's identity, creation timestamp, and {@code @Version}.
     *
     * <p>Used when persisting updates to an aggregate that was loaded,
     * mutated, and now needs flushing. Immutable Episode fields
     * ({@code id}, {@code number}, {@code title}, {@code airDate},
     * {@code createdAt}) are NOT copied because they cannot change
     * via aggregate behavior.
     *
     * @param episode the source aggregate (post-mutation)
     * @param entity  the loaded entity to update in place
     */
    public void applyTo(Episode episode, EpisodeEntity entity) {
        if (episode.theAbstract() != null) {
            entity.theAbstract = embeddableFor(episode.theAbstract());
        } else {
            entity.theAbstract = null;
        }
        entity.presenters.clear();
        entity.presenters.addAll(episode.presenters().stream()
            .map(PersonId::value)
            .toList());
        entity.speakers.clear();
        entity.speakers.addAll(episode.speakers().stream()
            .map(PersonId::value)
            .toList());
        entity.status = episode.status();
        entity.updatedAt = episode.updatedAt();
    }

    private AbstractEmbeddable embeddableFor(Abstract a) {
        AbstractEmbeddable e = new AbstractEmbeddable();
        e.abstractId = a.id().value();
        e.abstractText = a.text().value();
        e.abstractSubmittedAt = a.submittedAt();
        return e;
    }
}
