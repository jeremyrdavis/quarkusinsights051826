package io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.UUID;

/**
 * JPA embeddable representing a speaker (guest) summary stored in the
 * {@code public_episode_view_speaker} collection table.
 *
 * <p>Part of the Public Catalog bounded context, infrastructure layer.
 *
 * <p>Used with {@code @ElementCollection} on
 * {@link PublicEpisodeViewEntity#speakers} to store a flat list of
 * (personId, displayName) tuples without requiring a separate entity class.
 * This is the speaker counterpart to {@link PresenterEmbeddable}; keeping
 * them as distinct types prevents ambiguity in the entity mapping and
 * matches the distinction at the domain level between
 * {@link io.arrogantprogrammer.quarkusinsights.catalog.domain.PresenterSummary}
 * and
 * {@link io.arrogantprogrammer.quarkusinsights.catalog.domain.SpeakerSummary}.
 *
 * <p>Equality is defined purely by {@code personId} so that idempotent
 * projection logic can detect duplicate assignments. {@code hashCode} follows
 * the same convention so the embeddable works correctly inside a
 * {@link java.util.Set}.
 */
@Embeddable
public class SpeakerEmbeddable {

    @Column(name = "person_id", nullable = false)
    public UUID personId;

    @Column(name = "display_name", nullable = false, length = 210)
    public String displayName;

    /**
     * Default constructor required by JPA.
     */
    public SpeakerEmbeddable() {
    }

    /**
     * Creates a SpeakerEmbeddable.
     *
     * @param personId    the person's UUID; must not be null
     * @param displayName the person's display name; must not be null or blank
     */
    public SpeakerEmbeddable(UUID personId, String displayName) {
        if (personId == null) {
            throw new IllegalArgumentException("SpeakerEmbeddable.personId must not be null");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("SpeakerEmbeddable.displayName must not be null or blank");
        }
        this.personId = personId;
        this.displayName = displayName;
    }

    /**
     * Equality based on {@code personId} only, so that the same person can
     * only appear once in the speakers set.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpeakerEmbeddable other)) return false;
        return Objects.equals(personId, other.personId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(personId);
    }
}
