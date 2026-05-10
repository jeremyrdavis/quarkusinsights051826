package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

/**
 * Command to transition an Episode from SCHEDULED to LIVE.
 *
 * <p>Consumed by {@code GoLiveUseCase}.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param episodeId the target Episode's id; must not be null
 */
public record GoLiveCommand(EpisodeId episodeId) {

    /**
     * @throws IllegalArgumentException if {@code episodeId} is null
     */
    public GoLiveCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
    }
}
