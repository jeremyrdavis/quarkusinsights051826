package io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA persistence model for the Public Catalog's denormalized episode view.
 *
 * <p>Part of the Public Catalog bounded context, infrastructure layer.
 *
 * <p>This entity is the storage counterpart to the
 * {@link io.arrogantprogrammer.quarkusinsights.catalog.domain.PublicEpisodeView}
 * read-model record. It is maintained exclusively by three event projectors:
 * <ul>
 *   <li>{@code EpisodeProjector} — creates rows and updates lifecycle/cast fields</li>
 *   <li>{@code PersonProjector} — cascades name changes across all embedded
 *       presenter/speaker summaries</li>
 *   <li>{@code EngagementProjector} — increments {@code commentCount} and
 *       updates the rating running totals</li>
 * </ul>
 *
 * <p>Rating average design — running totals approach:
 * Rather than scanning all Rating rows on every request, the projection stores
 * {@code ratingSum} (the cumulative sum of all star values) and {@code ratingCount}
 * (the total number of ratings). The {@link #averageRating()} helper divides
 * them on demand. This keeps both the query and the projector update O(1) in
 * the number of ratings submitted.
 *
 * <p>{@code presenters} and {@code speakers} use {@code @ElementCollection}
 * backed by separate join tables. Each embeddable is equality-checked by
 * {@code personId} only (see {@link PresenterEmbeddable#equals} and
 * {@link SpeakerEmbeddable#equals}), so adding the same person twice is
 * idempotent at the Java collection level.
 *
 * <p>Extends {@link PanacheEntityBase} (not {@code PanacheEntity}) so the
 * {@code @Id} is an explicit {@code UUID} rather than an auto-generated
 * {@code Long}.
 */
@Entity
@Table(name = "public_episode_view")
public class PublicEpisodeViewEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(nullable = false)
    public int number;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(name = "air_date", nullable = false)
    public LocalDate airDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EpisodeStatus status;

    @Column(name = "abstract_text", nullable = true, length = 5000)
    public String abstractText;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "public_episode_view_presenter", joinColumns = @JoinColumn(name = "view_id"))
    public Set<PresenterEmbeddable> presenters = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "public_episode_view_speaker", joinColumns = @JoinColumn(name = "view_id"))
    public Set<SpeakerEmbeddable> speakers = new HashSet<>();

    @Column(name = "comment_count", nullable = false)
    public int commentCount = 0;

    /**
     * Running total of all star values submitted for this episode.
     * Null when no ratings have been submitted yet.
     * Used together with {@link #ratingCount} to compute {@link #averageRating()}.
     */
    @Column(name = "rating_sum", nullable = true, precision = 10, scale = 2)
    public BigDecimal ratingSum = null;

    @Column(name = "rating_count", nullable = false)
    public int ratingCount = 0;

    @Column(name = "last_updated_at", nullable = false)
    public Instant lastUpdatedAt;

    /**
     * Default constructor required by JPA.
     */
    public PublicEpisodeViewEntity() {
    }

    /**
     * Computes the average star rating from the running totals.
     *
     * <p>Returns {@code null} when no ratings have been submitted yet
     * ({@code ratingCount == 0}). The caller (mapper) wraps the result in
     * {@link java.util.Optional}.
     *
     * @return the average rounded to 2 decimal places, or {@code null}
     */
    public BigDecimal averageRating() {
        if (ratingCount == 0 || ratingSum == null) {
            return null;
        }
        return ratingSum.divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
    }
}
