package io.arrogantprogrammer.quarkusinsights.programming.domain;

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

    /**
     * Creates a MissingSpeaker exception.
     */
    public MissingSpeaker() {
        super("Episode has no speakers; assignSpeaker() must be called before publish()");
    }
}
