package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

/**
 * Unique identifier for an Episode aggregate.
 *
 * <p>A Value Object — immutable, equals-by-value, no identity beyond its
 * wrapped {@link UUID}. The Programming bounded context mints EpisodeIds
 * when scheduling new shows; the IDs then travel through domain events
 * to the Engagement context (where Comments and Ratings reference them)
 * and to the Public Catalog context (where the read-side projection
 * denormalizes them). Because more than one bounded context speaks of
 * EpisodeIds, the type lives in {@code shared/} rather than inside any
 * single context's {@code domain/} package.
 *
 * <p>Construct via the public canonical constructor, {@link #random()},
 * or {@link #fromString(String)}. EpisodeIds compare equal when their
 * wrapped UUIDs are equal — the same UUID parsed from two different
 * strings yields two equal EpisodeId instances.
 *
 * @param value the UUID that uniquely identifies this episode; must not be {@code null}
 */
public record EpisodeId(UUID value) {

    /**
     * Creates an EpisodeId wrapping the given UUID.
     *
     * @param value the wrapped UUID; must not be {@code null}
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public EpisodeId {
        if (value == null) {
            throw new IllegalArgumentException("EpisodeId value must not be null");
        }
    }

    /**
     * Mints a fresh EpisodeId backed by a new random {@link UUID}.
     *
     * @return a new EpisodeId distinct from any previously generated
     */
    public static EpisodeId random() {
        return new EpisodeId(UUID.randomUUID());
    }

    /**
     * Parses an EpisodeId from its canonical UUID string form.
     *
     * @param s a UUID string in the form
     *     {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}; must not be {@code null}
     * @return the EpisodeId wrapping the parsed UUID
     * @throws IllegalArgumentException if {@code s} is {@code null} or is
     *     not a valid UUID string
     */
    public static EpisodeId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("EpisodeId string must not be null");
        }
        return new EpisodeId(UUID.fromString(s));
    }
}
