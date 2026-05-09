package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

/**
 * Unique identifier for a Comment aggregate.
 *
 * <p>A Value Object — immutable, equals-by-value, no identity beyond its
 * wrapped {@link UUID}. The Engagement bounded context mints CommentIds
 * when audience members submit comments on published episodes. The Public
 * Catalog context observes {@code CommentSubmitted} events to keep its
 * denormalized comment counts current, but does not address Comments by
 * ID directly — that is why this type still belongs in {@code shared/}:
 * it appears in cross-context events.
 *
 * <p>Construct via the canonical constructor, {@link #random()}, or
 * {@link #fromString(String)}. CommentIds compare equal when their
 * wrapped UUIDs are equal.
 *
 * @param value the UUID that uniquely identifies this comment; must not be {@code null}
 */
public record CommentId(UUID value) {

    /**
     * Creates a CommentId wrapping the given UUID.
     *
     * @param value the wrapped UUID; must not be {@code null}
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public CommentId {
        if (value == null) {
            throw new IllegalArgumentException("CommentId value must not be null");
        }
    }

    /**
     * Mints a fresh CommentId backed by a new random {@link UUID}.
     *
     * @return a new CommentId distinct from any previously generated
     */
    public static CommentId random() {
        return new CommentId(UUID.randomUUID());
    }

    /**
     * Parses a CommentId from its canonical UUID string form.
     *
     * @param s a UUID string in the form
     *     {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}; must not be {@code null}
     * @return the CommentId wrapping the parsed UUID
     * @throws IllegalArgumentException if {@code s} is {@code null} or is
     *     not a valid UUID string
     */
    public static CommentId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("CommentId string must not be null");
        }
        return new CommentId(UUID.fromString(s));
    }
}
