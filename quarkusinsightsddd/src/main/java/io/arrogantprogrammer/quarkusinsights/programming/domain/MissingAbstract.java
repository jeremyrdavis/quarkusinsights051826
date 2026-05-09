package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

/**
 * Thrown when {@link Episode#publish()} is invoked on an episode that
 * has not yet had an abstract submitted.
 *
 * <p>An episode cannot be published without an abstract; the abstract
 * is the human-readable description that the public catalog displays.
 * The fix is to call {@link Episode#submitAbstract} before
 * {@link Episode#publish()}.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class MissingAbstract extends RuntimeException {

    private final EpisodeId episodeId;

    /**
     * Creates a MissingAbstract exception.
     *
     * @param episodeId the id of the episode that has no abstract
     */
    public MissingAbstract(EpisodeId episodeId) {
        super("Episode " + episodeId.value() + " has no abstract; submitAbstract() must be called before publish()");
        this.episodeId = episodeId;
    }

    /**
     * @return the id of the episode that triggered the exception
     */
    public EpisodeId episodeId() {
        return episodeId;
    }
}
