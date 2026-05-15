package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Thrown when a behavior method on {@link Episode} is invoked from an
 * {@link EpisodeStatus} that does not permit the operation.
 *
 * <p>For example, calling {@link Episode#publish()} on an episode that
 * is still {@link EpisodeStatus#SCHEDULED} (rather than {@code LIVE})
 * yields an {@code IllegalEpisodeTransition} carrying the actual
 * current status. The set of permitted source states is documented in
 * each behavior method's own Javadoc — this exception does not name
 * one because some operations are valid from more than one state
 * (e.g., {@code assignPresenter} is allowed from both {@code SCHEDULED}
 * and {@code LIVE}).
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

    /**
     * Creates an IllegalEpisodeTransition.
     *
     * @param actual the status the episode was in when the disallowed
     *               transition was attempted
     */
    public IllegalEpisodeTransition(EpisodeStatus actual) {
        super("Episode is " + actual + "; the requested transition is not allowed from this state");
        this.actual = actual;
    }

    /**
     * @return the status the episode was in when the disallowed
     *     transition was attempted
     */
    public EpisodeStatus actual() {
        return actual;
    }
}
