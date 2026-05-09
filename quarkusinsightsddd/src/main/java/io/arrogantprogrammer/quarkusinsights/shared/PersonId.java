package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

/**
 * Unique identifier for a Person aggregate.
 *
 * <p>A Value Object — immutable, equals-by-value, no identity beyond its
 * wrapped {@link UUID}. The People bounded context mints PersonIds when
 * registering hosts and guests; the Programming context references them
 * on Episodes (in the {@code presenters} and {@code speakers} sets), and
 * the Public Catalog context denormalizes them into per-episode summaries.
 * Because PersonIds cross bounded contexts, the type lives in
 * {@code shared/} rather than inside {@code people/domain/}.
 *
 * <p>Construct via the canonical constructor, {@link #random()}, or
 * {@link #fromString(String)}. PersonIds compare equal when their
 * wrapped UUIDs are equal.
 *
 * @param value the UUID that uniquely identifies this person; must not be {@code null}
 */
public record PersonId(UUID value) {

    /**
     * Creates a PersonId wrapping the given UUID.
     *
     * @param value the wrapped UUID; must not be {@code null}
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public PersonId {
        if (value == null) {
            throw new IllegalArgumentException("PersonId value must not be null");
        }
    }

    /**
     * Mints a fresh PersonId backed by a new random {@link UUID}.
     *
     * @return a new PersonId distinct from any previously generated
     */
    public static PersonId random() {
        return new PersonId(UUID.randomUUID());
    }

    /**
     * Parses a PersonId from its canonical UUID string form.
     *
     * @param s a UUID string in the form
     *     {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}; must not be {@code null}
     * @return the PersonId wrapping the parsed UUID
     * @throws IllegalArgumentException if {@code s} is {@code null} or is
     *     not a valid UUID string
     */
    public static PersonId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("PersonId string must not be null");
        }
        return new PersonId(UUID.fromString(s));
    }
}
