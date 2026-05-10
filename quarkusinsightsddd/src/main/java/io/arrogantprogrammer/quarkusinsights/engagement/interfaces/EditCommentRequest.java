package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

/**
 * Request body for editing an existing Comment within the 5-minute edit window.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 *
 * @param body the replacement comment body; must not be blank, max 2000 chars
 */
public record EditCommentRequest(String body) {
}
