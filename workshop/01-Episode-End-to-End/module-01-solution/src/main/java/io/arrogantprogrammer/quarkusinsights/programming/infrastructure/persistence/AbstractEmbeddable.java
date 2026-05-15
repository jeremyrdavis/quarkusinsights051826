package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA {@code @Embeddable} value mirroring the inside-aggregate
 * {@link io.arrogantprogrammer.quarkusinsights.programming.domain.Abstract}
 * domain entity.
 *
 * <p>Embedded inside {@link EpisodeEntity} rather than persisted as a
 * separate table because the Abstract has no separate identity at
 * the relational level — it is owned 1:1 by Episode and replaced
 * (not edited in place) when {@code Episode.submitAbstract} is
 * called again. All fields are nullable: an Episode without a
 * submitted abstract has all three columns null.
 *
 * <p>Part of the Programming bounded context, infrastructure layer.
 */
@Embeddable
public class AbstractEmbeddable {

    @Column(name = "abstract_id")
    public UUID abstractId;

    @Column(name = "abstract_text", length = 5000)
    public String abstractText;

    @Column(name = "abstract_submitted_at")
    public Instant abstractSubmittedAt;

    /**
     * Default constructor required by JPA.
     */
    public AbstractEmbeddable() {
    }
}
