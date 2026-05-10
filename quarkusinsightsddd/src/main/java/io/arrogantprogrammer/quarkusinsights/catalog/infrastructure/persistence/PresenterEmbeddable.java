package io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.UUID;

/**
 * JPA embeddable representing a presenter (host) summary stored in the
 * {@code public_episode_view_presenter} collection table.
 *
 * <p>Part of the Public Catalog bounded context, infrastructure layer.
 *
 * <p>Used with {@code @ElementCollection} on
 * {@link PublicEpisodeViewEntity#presenters} to store a flat list of
 * (personId, displayName) tuples without requiring a separate entity class.
 * The {@code personId} and {@code displayName} columns together form the
 * meaningful content of each row; there is no surrogate key.
 *
 * <p>Equality is defined purely by {@code personId} so that idempotent
 * projection logic can detect duplicate assignments (same person assigned
 * twice emits only one projector update). {@code hashCode} follows the same
 * convention so the embeddable works correctly inside a {@link java.util.Set}.
 */
@Embeddable
public class PresenterEmbeddable {

    @Column(name = "person_id", nullable = false)
    public UUID personId;

    @Column(name = "display_name", nullable = false, length = 210)
    public String displayName;

    /**
     * Default constructor required by JPA.
     */
    public PresenterEmbeddable() {
    }

    /**
     * Creates a PresenterEmbeddable.
     *
     * @param personId    the person's UUID; must not be null
     * @param displayName the person's display name; must not be null or blank
     */
    public PresenterEmbeddable(UUID personId, String displayName) {
        if (personId == null) {
            throw new IllegalArgumentException("PresenterEmbeddable.personId must not be null");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("PresenterEmbeddable.displayName must not be null or blank");
        }
        this.personId = personId;
        this.displayName = displayName;
    }

    /**
     * Equality based on {@code personId} only, so that the same person can
     * only appear once in the presenters set.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PresenterEmbeddable other)) return false;
        return Objects.equals(personId, other.personId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(personId);
    }
}
