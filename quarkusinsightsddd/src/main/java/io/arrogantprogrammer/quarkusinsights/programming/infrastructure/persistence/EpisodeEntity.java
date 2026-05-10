package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA persistence model for the Episode aggregate root.
 *
 * <p>Lives in the infrastructure layer — separate from the pure-POJO
 * domain {@link io.arrogantprogrammer.quarkusinsights.programming.domain.Episode}
 * to keep the domain free of any persistence imports (per design doc
 * §3.1, §4.4). The {@link EpisodeMapper} translates between the two.
 *
 * <p>Schema notes:
 * <ul>
 *   <li>{@code uk_episode_number}: UNIQUE constraint on {@code number}
 *       backing the cross-aggregate uniqueness rule (SPEC.md §3.6).
 *       The repository adapter catches the resulting
 *       {@code ConstraintViolationException} on {@code save} and
 *       translates it back into
 *       {@code EpisodeNumberAlreadyExists}.</li>
 *   <li>{@code episode_presenter} / {@code episode_speaker}: join
 *       tables for the {@code @ElementCollection<UUID>} sets, each
 *       with a composite UNIQUE on (episode_id, person_id) so the
 *       same person cannot appear twice in the same role on the
 *       same episode.</li>
 *   <li>The Abstract is embedded (no separate table) since it has
 *       no separate identity at the relational level.</li>
 *   <li>{@code @Version}: optimistic locking column for safe
 *       concurrent updates.</li>
 * </ul>
 *
 * <p>Extends {@link PanacheEntityBase} (not {@link io.quarkus.hibernate.orm.panache.PanacheEntity})
 * so the {@code @Id} is an explicit {@code UUID} rather than an
 * auto-generated {@code Long}.
 *
 * <p>Part of the Programming bounded context, infrastructure layer.
 */
@Entity
@Table(name = "episode", uniqueConstraints = {
    @UniqueConstraint(name = "uk_episode_number", columnNames = "number")
})
public class EpisodeEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(nullable = false)
    public int number;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(name = "air_date", nullable = false)
    public LocalDate airDate;

    @Embedded
    public AbstractEmbeddable theAbstract;

    @ElementCollection
    @CollectionTable(
        name = "episode_presenter",
        joinColumns = @JoinColumn(name = "episode_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_episode_presenter",
            columnNames = {"episode_id", "person_id"})
    )
    @Column(name = "person_id", nullable = false)
    public Set<UUID> presenters = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "episode_speaker",
        joinColumns = @JoinColumn(name = "episode_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_episode_speaker",
            columnNames = {"episode_id", "person_id"})
    )
    @Column(name = "person_id", nullable = false)
    public Set<UUID> speakers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EpisodeStatus status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Version
    public long version;

    /**
     * Default constructor required by JPA.
     */
    public EpisodeEntity() {
    }
}
