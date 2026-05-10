package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Translates {@link Rating} aggregates into wire-friendly
 * {@link RatingResponse}s for HTTP responses. Unwraps domain VOs
 * so no domain types leak across the JSON boundary.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 */
@ApplicationScoped
public class RatingResponseMapper {

    /**
     * Builds a flat {@link RatingResponse} from a Rating.
     *
     * @param rating the source aggregate; must not be null
     * @return the wire-shaped response
     */
    public RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
            rating.id().value(),
            rating.episodeId().value(),
            rating.author().value(),
            rating.stars().value(),
            rating.submittedAt()
        );
    }
}
