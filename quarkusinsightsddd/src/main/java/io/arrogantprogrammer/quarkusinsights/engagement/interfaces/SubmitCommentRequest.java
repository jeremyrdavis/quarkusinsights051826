package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import java.util.UUID;

/**
 * Request body for submitting a new Comment on a published episode.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 *
 * @param episodeId    the id of the episode to comment on; must not be null
 * @param authorHandle the commenter's handle; must be 2–40 chars, alphanumeric + _-
 * @param body         the comment text; must not be blank, max 2000 chars
 */
public record SubmitCommentRequest(UUID episodeId, String authorHandle, String body) {
}
