package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Thrown when a behavior method on {@link Episode} is invoked from an
 * incompatible {@link EpisodeStatus}.
 *
 * <p>For example, calling {@link Episode#publish()} on an episode that
 * is still {@link EpisodeStatus#SCHEDULED} (not {@code LIVE}) yields
 * an {@code IllegalEpisodeTransition} carrying both the actual current
 * status and the status that would have made the call valid.
 *
 * <p>This exception is unchecked because lifecycle violations are
 * programmer errors, not recoverable conditions; callers MUST either
 * inspect the status before invoking the behavior or accept the
 * exception as a 409 Conflict at the REST boundary.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class IllegalEpisodeTransition extends RuntimeException {

    private final EpisodeStatus actual;
    private final EpisodeStatus required;

    /**
     * Creates an IllegalEpisodeTransition.
     *
     * @param actual   the status the episode is actually in
     * @param required the status that would have made the call valid
     */
    public IllegalEpisodeTransition(EpisodeStatus actual, EpisodeStatus required) {
        super("Episode is " + actual + ", but the requested transition requires " + required);
        this.actual = actual;
        this.required = required;
    }

    /**
     * @return the status the episode is actually in
     */
    public EpisodeStatus actual() {
        return actual;
    }

    /**
     * @return the status the call required
     */
    public EpisodeStatus required() {
        return required;
    }
}
