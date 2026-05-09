package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Thrown when {@link Episode#publish()} is invoked on an episode that
 * has zero presenters assigned.
 *
 * <p>An episode cannot be published without at least one presenter
 * (host). The fix is to call
 * {@link Episode#assignPresenter(io.arrogantprogrammer.quarkusinsights.shared.PersonId)}
 * at least once before {@link Episode#publish()}.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class MissingPresenter extends RuntimeException {

    /**
     * Creates a MissingPresenter exception.
     */
    public MissingPresenter() {
        super("Episode has no presenters; assignPresenter() must be called before publish()");
    }
}
