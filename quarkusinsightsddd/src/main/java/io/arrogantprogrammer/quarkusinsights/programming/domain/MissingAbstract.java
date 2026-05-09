package io.arrogantprogrammer.quarkusinsights.programming.domain;

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

    /**
     * Creates a MissingAbstract exception.
     */
    public MissingAbstract() {
        super("Episode has no abstract; submitAbstract() must be called before publish()");
    }
}
