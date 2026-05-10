package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentNotFound;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentRepository;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.EditWindowExpired;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.application.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Application service for the Comment aggregate.
 *
 * <p>Orchestrates two Comment operations: submitting a new comment and
 * editing an existing one within the 5-minute window. Each method is a
 * transactional unit of work that creates or loads the aggregate, invokes
 * its behavior, persists, and dispatches the recorded domain events.
 *
 * <p>The service contains no business rules — those live in the
 * {@link Comment} aggregate. Specifically, the 5-minute edit window is
 * enforced by {@link Comment#edit(io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody)};
 * this service propagates {@link EditWindowExpired} unchanged.
 *
 * <p>Part of the Engagement bounded context, application layer.
 */
@ApplicationScoped
public class CommentService {

    private final CommentRepository commentRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates the service.
     *
     * @param commentRepository the Comment repository port
     * @param eventPublisher    the domain event publisher port
     */
    @Inject
    public CommentService(CommentRepository commentRepository,
                          DomainEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Submits a new Comment on a published episode and publishes
     * a {@link io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentSubmitted}
     * domain event.
     *
     * @param cmd the submit command; must not be null
     * @return the id of the freshly submitted Comment
     */
    @Transactional
    public CommentId submit(SubmitCommentCommand cmd) {
        Comment comment = Comment.submit(cmd.episodeId(), cmd.author(), cmd.body());
        commentRepository.save(comment);
        dispatchEvents(comment);
        return comment.id();
    }

    /**
     * Edits the body of an existing Comment within the 5-minute edit window,
     * then publishes a {@link io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentEdited}
     * domain event.
     *
     * @param cmd the edit command; must not be null
     * @throws CommentNotFound    if no Comment with {@code cmd.commentId()} exists
     * @throws EditWindowExpired  if more than 5 minutes have elapsed since the
     *                            comment was submitted (propagated from the aggregate)
     */
    @Transactional
    public void edit(EditCommentCommand cmd) {
        Comment comment = loadOrThrow(cmd.commentId());
        comment.edit(cmd.body());
        commentRepository.save(comment);
        dispatchEvents(comment);
    }

    private Comment loadOrThrow(CommentId id) {
        return commentRepository.findById(id)
            .orElseThrow(() -> new CommentNotFound(id));
    }

    private void dispatchEvents(Comment comment) {
        List<DomainEvent> events = comment.recordedEvents();
        comment.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
