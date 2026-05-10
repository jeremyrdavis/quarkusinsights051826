package io.arrogantprogrammer.quarkusinsights.programming.domain;

import java.time.Instant;

/**
 * The submitted abstract for an Episode — an inside-aggregate entity
 * owned exclusively by {@link Episode}.
 *
 * <p>An Abstract has identity ({@link AbstractId}) but is immutable:
 * once submitted, it cannot be edited in place. Replacing the abstract
 * (via {@link Episode#submitAbstract} called again before the episode
 * goes live) creates a new Abstract with a new id and timestamp; the
 * previous instance is discarded.
 *
 * <p>This type is a record with structural equality. Within the
 * aggregate boundary that is acceptable because the aggregate is the
 * only thing that creates or replaces Abstracts; outside callers never
 * see two Abstract instances they need to compare by reference identity.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param id           the abstract's identifier
 * @param text         the body of the abstract
 * @param submittedAt  the instant the abstract was first submitted
 */
public record Abstract(AbstractId id, AbstractText text, Instant submittedAt) {

    /**
     * Creates an Abstract.
     *
     * @param id          the identifier; must not be null
     * @param text        the body; must not be null
     * @param submittedAt the submission timestamp; must not be null
     * @throws IllegalArgumentException if any argument is null
     */
    public Abstract {
        if (id == null) {
            throw new IllegalArgumentException("Abstract id must not be null");
        }
        if (text == null) {
            throw new IllegalArgumentException("Abstract text must not be null");
        }
        if (submittedAt == null) {
            throw new IllegalArgumentException("Abstract submittedAt must not be null");
        }
    }
}
