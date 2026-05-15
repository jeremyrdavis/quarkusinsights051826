package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

/**
 * Command to publish a LIVE Episode.
 *
 * <p>Consumed by {@code PublishEpisodeUseCase}. Requires the Episode
 * to satisfy the publish preconditions: status LIVE, abstract present,
 * at least one presenter, at least one speaker.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param episodeId the target Episode's id; must not be null
 */
public record PublishEpisodeCommand(EpisodeId episodeId) {

    /**
     * @throws IllegalArgumentException if {@code episodeId} is null
     */
    public PublishEpisodeCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
    }
}
