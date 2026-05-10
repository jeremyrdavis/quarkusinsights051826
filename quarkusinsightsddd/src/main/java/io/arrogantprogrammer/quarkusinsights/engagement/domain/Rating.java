package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Rating aggregate root — represents a star rating (1–5) submitted by
 * an audience member on a published QuarkusInsights episode.
 *
 * <p><strong>Immutability.</strong> A Rating is immutable after construction.
 * All fields are final; there are no mutating behavior methods. The
 * {@link #submit} factory is the only way to create a Rating through normal
 * flow.
 *
 * <p><strong>Uniqueness invariant.</strong> The "one Rating per
 * (EpisodeId, AuthorHandle)" invariant is enforced at three places (see
 * SPEC.md §3.6): the {@code RatingService} precheck, the database UNIQUE
 * constraint, and the repository adapter's exception translation. This
 * aggregate itself does not enforce that constraint — it cannot observe
 * other aggregates.
 *
 * <p><strong>Domain events.</strong> {@link #submit} records a
 * {@link RatingSubmitted} event in the aggregate's internal
 * {@code recordedEvents} list. The application layer reads the list via
 * {@link #recordedEvents()} after persisting, dispatches the events via the
 * {@code DomainEventPublisher}, then calls {@link #clearRecordedEvents()} to
 * reset.
 *
 * <p><strong>Construction.</strong> {@link #submit} is the only way to
 * create a Rating through normal flow. {@link #rehydrate} exists solely for
 * the persistence adapter; application code MUST NOT call it.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 */
public class Rating {

    private final RatingId id;
    private final EpisodeId episodeId;
    private final AuthorHandle author;
    private final Stars stars;
    private final Instant submittedAt;
    private final List<DomainEvent> recordedEvents = new ArrayList<>();

    private Rating(RatingId id, EpisodeId episodeId, AuthorHandle author,
                   Stars stars, Instant submittedAt) {
        this.id = id;
        this.episodeId = episodeId;
        this.author = author;
        this.stars = stars;
        this.submittedAt = submittedAt;
    }

    /**
     * Submits a new Rating and records a {@link RatingSubmitted} domain event.
     *
     * @param episodeId the id of the episode being rated; must not be null
     * @param author    the handle of the rater; must not be null
     * @param stars     the star rating; must not be null
     * @return a new Rating with a randomly generated id
     */
    public static Rating submit(EpisodeId episodeId, AuthorHandle author, Stars stars) {
        Instant now = Instant.now();
        RatingId id = RatingId.random();
        Rating rating = new Rating(id, episodeId, author, stars, now);
        rating.recordedEvents.add(new RatingSubmitted(id, episodeId, author, stars, now));
        return rating;
    }

    /**
     * Reconstructs a Rating from its persisted state.
     *
     * <p><strong>For persistence adapter use only.</strong> Application
     * code MUST NOT call this — use {@link #submit} or load via the
     * {@link RatingRepository}. Rehydration does not record any domain
     * event because no domain action is occurring.
     *
     * @param id          the persisted rating id
     * @param episodeId   the persisted episode id
     * @param author      the persisted author handle
     * @param stars       the persisted star value
     * @param submittedAt the persisted submission timestamp
     * @return a hydrated Rating whose recordedEvents list is empty
     */
    public static Rating rehydrate(RatingId id, EpisodeId episodeId, AuthorHandle author,
                                   Stars stars, Instant submittedAt) {
        return new Rating(id, episodeId, author, stars, submittedAt);
    }

    /** @return the aggregate's identifier */
    public RatingId id() { return id; }

    /** @return the id of the episode this rating was submitted on */
    public EpisodeId episodeId() { return episodeId; }

    /** @return the handle of the audience member who submitted this rating */
    public AuthorHandle author() { return author; }

    /** @return the star rating value */
    public Stars stars() { return stars; }

    /** @return the instant the rating was submitted */
    public Instant submittedAt() { return submittedAt; }

    /**
     * @return an unmodifiable snapshot of domain events recorded by
     *     this aggregate since the last {@link #clearRecordedEvents()}
     */
    public List<DomainEvent> recordedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recordedEvents));
    }

    /**
     * Clears the aggregate's recorded-events list. The application
     * layer calls this after publishing the events so subsequent
     * behavior calls do not redeliver the same events.
     */
    public void clearRecordedEvents() {
        recordedEvents.clear();
    }
}
