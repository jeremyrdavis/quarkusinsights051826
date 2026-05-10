package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Translates between the {@link Rating} domain aggregate and the
 * {@link RatingEntity} JPA persistence model.
 *
 * <p>The mapper is the single bridge between the two worlds: the
 * domain knows nothing about JPA, and the entity knows nothing about
 * domain methods. Construction direction:
 * <ul>
 *   <li>{@link #toEntity}: produces a fresh {@code RatingEntity} from a
 *       Rating, suitable for {@code persist()}.</li>
 *   <li>{@link #toDomain}: produces a fresh {@link Rating} from a loaded
 *       entity using {@link Rating#rehydrate} (does not record any domain
 *       events).</li>
 * </ul>
 *
 * <p><strong>Note on {@code applyTo}.</strong> {@link Rating} is immutable
 * after construction — all fields are final and there are no mutating
 * behavior methods. Therefore no {@code applyTo} method exists; the
 * {@link RatingRepositoryImpl} always inserts new entities and never
 * updates existing ones.
 *
 * <p>Part of the Engagement bounded context, infrastructure layer.
 */
@ApplicationScoped
public class RatingMapper {

    /**
     * Builds a fresh {@link RatingEntity} mirroring the given {@link Rating}.
     * Used when persisting a new aggregate.
     *
     * @param rating the source aggregate; must not be null
     * @return a fresh entity with all fields populated
     */
    public RatingEntity toEntity(Rating rating) {
        RatingEntity entity = new RatingEntity();
        entity.id = rating.id().value();
        entity.episodeId = rating.episodeId().value();
        entity.authorHandle = rating.author().value();
        entity.stars = rating.stars().value();
        entity.submittedAt = rating.submittedAt();
        return entity;
    }

    /**
     * Reconstructs a {@link Rating} aggregate from its persisted state.
     * Uses {@link Rating#rehydrate} so no domain events are recorded —
     * load is not a domain action.
     *
     * @param entity the loaded entity; must not be null
     * @return the rehydrated Rating
     */
    public Rating toDomain(RatingEntity entity) {
        return Rating.rehydrate(
            new RatingId(entity.id),
            new EpisodeId(entity.episodeId),
            new AuthorHandle(entity.authorHandle),
            new Stars(entity.stars),
            entity.submittedAt
        );
    }
}
