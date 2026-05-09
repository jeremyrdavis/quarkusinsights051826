package io.arrogantprogrammer.quarkusinsights.programming.domain;

import java.util.UUID;

/**
 * Unique identifier for an {@link Abstract} inside-aggregate entity
 * within the Programming context.
 *
 * <p>A Value Object — immutable, equals-by-value, no identity beyond
 * its wrapped {@link UUID}. Unlike the shared/ ID VOs (e.g.,
 * {@link io.arrogantprogrammer.quarkusinsights.shared.EpisodeId}),
 * AbstractId lives inside {@code programming/domain/} because the
 * Abstract entity is owned exclusively by Episode and never travels
 * across bounded contexts.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * <p>Construct via the canonical constructor, {@link #random()}, or
 * {@link #fromString(String)}. AbstractIds compare equal when their
 * wrapped UUIDs are equal.
 *
 * @param value the wrapped UUID; must not be null
 */
public record AbstractId(UUID value) {

    /**
     * Creates an AbstractId wrapping the given UUID.
     *
     * @param value the wrapped UUID; must not be null
     * @throws IllegalArgumentException if {@code value} is null
     */
    public AbstractId {
        if (value == null) {
            throw new IllegalArgumentException("AbstractId value must not be null");
        }
    }

    /**
     * Mints a fresh AbstractId backed by a new random {@link UUID}.
     *
     * @return a new AbstractId distinct from any previously generated
     */
    public static AbstractId random() {
        return new AbstractId(UUID.randomUUID());
    }

    /**
     * Parses an AbstractId from its canonical UUID string form.
     *
     * @param s a UUID string in the form
     *     {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}; must not be null
     * @return the AbstractId wrapping the parsed UUID
     * @throws IllegalArgumentException if {@code s} is null or not a
     *     valid UUID string
     */
    public static AbstractId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("AbstractId string must not be null");
        }
        return new AbstractId(UUID.fromString(s));
    }
}
