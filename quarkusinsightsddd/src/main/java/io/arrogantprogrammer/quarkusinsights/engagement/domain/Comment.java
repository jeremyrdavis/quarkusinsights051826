package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Comment aggregate root — represents a text comment submitted by an
 * audience member on a published QuarkusInsights episode.
 *
 * <p><strong>Lifecycle.</strong> A Comment is created by the
 * {@link #submit} factory method and may be edited once via {@link #edit}
 * within a 5-minute window after submission. There is no delete or
 * archive operation in this bounded context — those are operational
 * concerns handled outside the domain model.
 *
 * <p><strong>Edit window.</strong> The 5-minute edit window is enforced
 * by comparing {@link Duration#between(Instant, Instant)} of
 * {@code submittedAt} to {@link Instant#now()} against
 * {@link Duration#ofMinutes(5)}. Expired edits throw
 * {@link EditWindowExpired}.
 *
 * <p><strong>Domain events.</strong> Each state-mutating factory or
 * behavior appends a {@link DomainEvent} to the aggregate's internal
 * {@code recordedEvents} list. The application layer reads the list via
 * {@link #recordedEvents()} after persisting, dispatches the events via
 * the {@code DomainEventPublisher}, then calls
 * {@link #clearRecordedEvents()} to reset.
 *
 * <p><strong>Construction.</strong> {@link #submit} is the only way to
 * create a Comment through normal flow. {@link #rehydrate} exists solely
 * for the persistence adapter to reconstruct a Comment from stored state
 * without recording an event; application code MUST NOT call it.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 */
public class Comment {

    private final CommentId id;
    private final EpisodeId episodeId;
    private final AuthorHandle author;
    private final Instant submittedAt;
    private CommentBody body;
    private final List<DomainEvent> recordedEvents = new ArrayList<>();

    private Comment(CommentId id, EpisodeId episodeId, AuthorHandle author,
                    CommentBody body, Instant submittedAt) {
        this.id = id;
        this.episodeId = episodeId;
        this.author = author;
        this.body = body;
        this.submittedAt = submittedAt;
    }

    /**
     * Submits a new Comment and records a {@link CommentSubmitted} domain event.
     *
     * @param episodeId the id of the episode being commented on; must not be null
     * @param author    the handle of the commenter; must not be null
     * @param body      the comment text; must not be null
     * @return a new Comment with a randomly generated id
     */
    public static Comment submit(EpisodeId episodeId, AuthorHandle author, CommentBody body) {
        Instant now = Instant.now();
        CommentId id = CommentId.random();
        Comment comment = new Comment(id, episodeId, author, body, now);
        comment.recordedEvents.add(new CommentSubmitted(id, episodeId, author, now));
        return comment;
    }

    /**
     * Reconstructs a Comment from its persisted state.
     *
     * <p><strong>For persistence adapter use only.</strong> Application
     * code MUST NOT call this — use {@link #submit} or load via the
     * {@link CommentRepository}. Rehydration does not record any domain
     * event because no domain action is occurring.
     *
     * @param id          the persisted comment id
     * @param episodeId   the persisted episode id
     * @param author      the persisted author handle
     * @param body        the persisted comment body
     * @param submittedAt the persisted submission timestamp
     * @return a hydrated Comment whose recordedEvents list is empty
     */
    public static Comment rehydrate(CommentId id, EpisodeId episodeId, AuthorHandle author,
                                    CommentBody body, Instant submittedAt) {
        return new Comment(id, episodeId, author, body, submittedAt);
    }

    /**
     * Edits the body of this comment, provided the 5-minute edit window
     * has not yet elapsed since submission.
     *
     * <p>Records a {@link CommentEdited} domain event on success.
     *
     * @param newBody the replacement comment body; must not be null
     * @throws IllegalArgumentException if {@code newBody} is null
     * @throws EditWindowExpired if more than 5 minutes have elapsed since
     *     {@link #submittedAt()}
     */
    public void edit(CommentBody newBody) {
        if (newBody == null) {
            throw new IllegalArgumentException("CommentBody must not be null");
        }
        if (Duration.between(submittedAt, Instant.now()).compareTo(Duration.ofMinutes(5)) > 0) {
            throw new EditWindowExpired(id, submittedAt);
        }
        Instant now = Instant.now();
        this.body = newBody;
        recordedEvents.add(new CommentEdited(id, now));
    }

    /** @return the aggregate's identifier */
    public CommentId id() { return id; }

    /** @return the id of the episode this comment was submitted on */
    public EpisodeId episodeId() { return episodeId; }

    /** @return the handle of the audience member who submitted this comment */
    public AuthorHandle author() { return author; }

    /** @return the current comment body */
    public CommentBody body() { return body; }

    /** @return the instant the comment was submitted */
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
