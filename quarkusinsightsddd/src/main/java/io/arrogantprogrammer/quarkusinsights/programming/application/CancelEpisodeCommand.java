package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

/**
 * Command to cancel a SCHEDULED Episode.
 *
 * <p>Consumed by {@code CancelEpisodeUseCase}. Cancellation is only
 * permitted before the Episode goes live.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param episodeId the target Episode's id; must not be null
 * @param reason    a human-readable cancellation reason; must not be
 *                  null or blank
 */
public record CancelEpisodeCommand(EpisodeId episodeId, String reason) {

    /**
     * @throws IllegalArgumentException if {@code episodeId} is null
     *     or {@code reason} is null/blank
     */
    public CancelEpisodeCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be null or blank");
        }
    }
}
