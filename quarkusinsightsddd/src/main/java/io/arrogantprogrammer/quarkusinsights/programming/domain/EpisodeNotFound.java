package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

/**
 * Thrown when a use case attempts to load an Episode aggregate by id
 * and {@link EpisodeRepository#findById} returns empty.
 *
 * <p>This exception is unchecked because not-found from a repository
 * is typically a programmer error or an invalid client request — the
 * application layer translates it (via REST exception mappers in Plan
 * 2C) to a 404 Not Found.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class EpisodeNotFound extends RuntimeException {

    private final EpisodeId episodeId;

    /**
     * Creates an EpisodeNotFound exception.
     *
     * @param episodeId the id that was not found
     */
    public EpisodeNotFound(EpisodeId episodeId) {
        super("Episode " + episodeId.value() + " not found");
        this.episodeId = episodeId;
    }

    /**
     * @return the id that was not found
     */
    public EpisodeId episodeId() {
        return episodeId;
    }
}
