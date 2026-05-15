package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

/**
 * Unique identifier for a Rating aggregate.
 *
 * <p>A Value Object — immutable, equals-by-value, no identity beyond its
 * wrapped {@link UUID}. The Engagement bounded context mints RatingIds
 * when audience members rate published episodes.
 *
 * <p>RatingId addresses an individual Rating; it is <em>not</em> the
 * mechanism that enforces the "one Rating per (EpisodeId, AuthorHandle)"
 * uniqueness invariant. That invariant is enforced at the use-case level
 * (and backed by a database UNIQUE constraint) — see SPEC.md §3.6.
 *
 * <p>Construct via the canonical constructor, {@link #random()}, or
 * {@link #fromString(String)}. RatingIds compare equal when their
 * wrapped UUIDs are equal.
 *
 * @param value the UUID that uniquely identifies this rating; must not be {@code null}
 */
public record RatingId(UUID value) {

    /**
     * Creates a RatingId wrapping the given UUID.
     *
     * @param value the wrapped UUID; must not be {@code null}
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public RatingId {
        if (value == null) {
            throw new IllegalArgumentException("RatingId value must not be null");
        }
    }

    /**
     * Mints a fresh RatingId backed by a new random {@link UUID}.
     *
     * @return a new RatingId distinct from any previously generated
     */
    public static RatingId random() {
        return new RatingId(UUID.randomUUID());
    }

    /**
     * Parses a RatingId from its canonical UUID string form.
     *
     * @param s a UUID string in the form
     *     {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}; must not be {@code null}
     * @return the RatingId wrapping the parsed UUID
     * @throws IllegalArgumentException if {@code s} is {@code null} or is
     *     not a valid UUID string
     */
    public static RatingId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("RatingId string must not be null");
        }
        return new RatingId(UUID.fromString(s));
    }
}
