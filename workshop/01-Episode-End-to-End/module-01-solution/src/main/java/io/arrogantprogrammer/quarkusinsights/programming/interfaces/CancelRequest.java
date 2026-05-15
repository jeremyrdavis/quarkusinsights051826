package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

/**
 * Wire DTO for {@code POST /episodes/{id}/cancel}.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 *
 * @param reason the cancellation reason (non-blank; validated by CancelEpisodeCommand)
 */
public record CancelRequest(String reason) {
}
