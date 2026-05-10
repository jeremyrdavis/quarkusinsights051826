package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence model for the Rating aggregate root.
 *
 * <p>Lives in the infrastructure layer — separate from the pure-POJO domain
 * {@link io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating} to
 * keep the domain free of any persistence imports (per design doc §3.1, §4.4).
 * The {@link RatingMapper} translates between the two.
 *
 * <p>Schema notes:
 * <ul>
 *   <li>{@code uk_rating_episode_author}: UNIQUE constraint on
 *       {@code (episode_id, author_handle)} — this is the <strong>second</strong>
 *       of three places where SPEC.md §3.6's "one Rating per (EpisodeId,
 *       AuthorHandle)" rule is enforced. The first place is the precheck in
 *       {@code RatingService.submit}; the third is {@code RatingRepositoryImpl}'s
 *       constraint-translation that catches the resulting
 *       {@code ConstraintViolationException} and re-throws
 *       {@link io.arrogantprogrammer.quarkusinsights.engagement.domain.AlreadyRated}.
 *       <strong>IMPORTANT:</strong> the constraint is declared table-level only
 *       ({@code @Table(uniqueConstraints = ...)}); there is NO {@code @Column(unique=true)}
 *       on {@code episodeId} or {@code authorHandle} to avoid Hibernate generating
 *       a duplicate auto-named constraint that would bypass the named-constraint
 *       translation (Plan 2C lesson).</li>
 *   <li>{@code @Version}: optimistic locking column. Rating is immutable after
 *       creation so in practice this column always remains at its initial value;
 *       it is retained for consistency with the project's entity pattern.</li>
 * </ul>
 *
 * <p>Extends {@link PanacheEntityBase} (not {@code PanacheEntity}) so the
 * {@code @Id} is an explicit {@code UUID} rather than an auto-generated
 * {@code Long}.
 *
 * <p>Part of the Engagement bounded context, infrastructure layer. See SPEC.md §3.6.
 */
@Entity
@Table(
    name = "rating",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_rating_episode_author",
        columnNames = {"episode_id", "author_handle"}
    )
)
public class RatingEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(name = "episode_id", nullable = false)
    public UUID episodeId;

    @Column(name = "author_handle", nullable = false, length = 40)
    public String authorHandle;

    @Column(nullable = false)
    public int stars;

    @Column(name = "submitted_at", nullable = false)
    public Instant submittedAt;

    @Version
    public long version;

    /**
     * Default constructor required by JPA.
     */
    public RatingEntity() {
    }
}
