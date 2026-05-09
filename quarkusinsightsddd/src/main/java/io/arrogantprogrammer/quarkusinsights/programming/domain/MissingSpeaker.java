package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

/**
 * Thrown when {@link Episode#publish()} is invoked on an episode that
 * has zero speakers assigned.
 *
 * <p>An episode cannot be published without at least one speaker
 * (guest). The fix is to call
 * {@link Episode#assignSpeaker(io.arrogantprogrammer.quarkusinsights.shared.PersonId)}
 * at least once before {@link Episode#publish()}.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class MissingSpeaker extends RuntimeException {

    private final EpisodeId episodeId;

    /**
     * Creates a MissingSpeaker exception.
     *
     * @param episodeId the id of the episode that has no speakers
     */
    public MissingSpeaker(EpisodeId episodeId) {
        super("Episode " + episodeId.value() + " has no speakers; assignSpeaker() must be called before publish()");
        this.episodeId = episodeId;
    }

    /**
     * @return the id of the episode that triggered the exception
     */
    public EpisodeId episodeId() {
        return episodeId;
    }
}
