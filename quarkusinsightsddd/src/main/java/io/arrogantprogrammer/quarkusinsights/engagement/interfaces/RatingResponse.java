package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import java.time.Instant;
import java.util.UUID;

/**
 * Flat JSON response shape for a Rating aggregate.
 *
 * <p>Designed for direct Jackson serialization — all components are
 * primitives or basic JDK types; no domain types leak across the wire.
 * {@link RatingResponseMapper} produces these from a loaded {@code Rating}
 * aggregate.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 *
 * @param id           the Rating's id
 * @param episodeId    the id of the episode this rating was submitted on
 * @param authorHandle the handle of the audience member who submitted the rating
 * @param stars        the star count (1–5)
 * @param submittedAt  the instant the rating was submitted
 */
public record RatingResponse(
    UUID id,
    UUID episodeId,
    String authorHandle,
    int stars,
    Instant submittedAt
) {
}
