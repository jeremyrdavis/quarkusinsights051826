package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import java.util.UUID;

/**
 * Request body for submitting a star Rating on a published episode.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 *
 * @param episodeId    the id of the episode to rate; must not be null
 * @param authorHandle the rater's handle; must be 2–40 chars, alphanumeric + _-
 * @param stars        the star count; must be in [1, 5]
 */
public record SubmitRatingRequest(UUID episodeId, String authorHandle, int stars) {
}
