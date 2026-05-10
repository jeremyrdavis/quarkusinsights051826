package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence model for the Comment aggregate root.
 *
 * <p>Lives in the infrastructure layer — separate from the pure-POJO domain
 * {@link io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment} to
 * keep the domain free of any persistence imports (per design doc §3.1, §4.4).
 * The {@link CommentMapper} translates between the two.
 *
 * <p>Schema notes:
 * <ul>
 *   <li>No UNIQUE constraint beyond the primary key — Comments are unique
 *       by id only; the "one Rating per (episode, author)" constraint belongs
 *       to {@link RatingEntity}.</li>
 *   <li>{@code episode_id} is indexed implicitly via the {@link #episodeId}
 *       column; queries by episode use a Panache JPQL predicate.</li>
 *   <li>{@code @Version}: optimistic locking column for safe concurrent
 *       updates during the 5-minute edit window.</li>
 * </ul>
 *
 * <p>Extends {@link PanacheEntityBase} (not {@code PanacheEntity}) so the
 * {@code @Id} is an explicit {@code UUID} rather than an auto-generated
 * {@code Long}.
 *
 * <p>Part of the Engagement bounded context, infrastructure layer.
 */
@Entity
@Table(name = "comment")
public class CommentEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(name = "episode_id", nullable = false)
    public UUID episodeId;

    @Column(name = "author_handle", nullable = false, length = 40)
    public String authorHandle;

    @Column(nullable = false, length = 2000)
    public String body;

    @Column(name = "submitted_at", nullable = false)
    public Instant submittedAt;

    @Version
    public long version;

    /**
     * Default constructor required by JPA.
     */
    public CommentEntity() {
    }
}
