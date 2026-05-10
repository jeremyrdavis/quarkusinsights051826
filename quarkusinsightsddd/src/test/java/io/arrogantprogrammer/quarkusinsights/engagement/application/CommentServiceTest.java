package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentEdited;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentNotFound;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentSubmitted;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.EditWindowExpired;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.application.RecordingDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link CommentService} behavior using stub implementations
 * of the {@code CommentRepository} and {@code DomainEventPublisher}
 * ports — no Quarkus startup needed.
 *
 * <p>Tests are grouped by service method via {@link Nested} classes.
 */
class CommentServiceTest {

    private static final EpisodeId EPISODE_ID = EpisodeId.random();
    private static final AuthorHandle AUTHOR = new AuthorHandle("alice");
    private static final CommentBody BODY = new CommentBody("Great episode!");

    private InMemoryCommentRepository repository;
    private RecordingDomainEventPublisher publisher;
    private CommentService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCommentRepository();
        publisher = new RecordingDomainEventPublisher();
        service = new CommentService(repository, publisher);
    }

    @Nested
    class Submit {

        @Test
        void persistsAndReturnsId() {
            SubmitCommentCommand cmd = new SubmitCommentCommand(EPISODE_ID, AUTHOR, BODY);
            CommentId id = service.submit(cmd);
            assertNotNull(id);
            assertEquals(1, repository.size());
            Comment persisted = repository.findById(id).orElseThrow();
            assertEquals(EPISODE_ID, persisted.episodeId());
            assertEquals(AUTHOR, persisted.author());
            assertEquals(BODY, persisted.body());
        }

        @Test
        void publishesCommentSubmittedEvent() {
            service.submit(new SubmitCommentCommand(EPISODE_ID, AUTHOR, BODY));
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(CommentSubmitted.class, publisher.publishedEvents().get(0));
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            CommentId id = service.submit(new SubmitCommentCommand(EPISODE_ID, AUTHOR, BODY));
            Comment persisted = repository.findById(id).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }

    @Nested
    class Edit {

        @Test
        void throwsCommentNotFoundWhenMissing() {
            CommentId missing = CommentId.random();
            CommentNotFound ex = assertThrows(CommentNotFound.class,
                () -> service.edit(new EditCommentCommand(missing, BODY)));
            assertEquals(missing, ex.commentId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void happyPathPersistsAndPublishesEvent() {
            // Submit a comment first
            CommentId id = service.submit(new SubmitCommentCommand(EPISODE_ID, AUTHOR, BODY));
            // Reset publisher to isolate the edit assertion
            publisher = new RecordingDomainEventPublisher();
            service = new CommentService(repository, publisher);

            CommentBody updatedBody = new CommentBody("Updated comment.");
            service.edit(new EditCommentCommand(id, updatedBody));

            Comment persisted = repository.findById(id).orElseThrow();
            assertEquals(updatedBody, persisted.body());
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(CommentEdited.class, publisher.publishedEvents().get(0));
        }

        @Test
        void propagatesEditWindowExpiredForOldComment() {
            // Seed the repository directly with a rehydrated Comment submitted 6 minutes ago
            CommentId id = CommentId.random();
            Instant oldSubmittedAt = Instant.now().minus(Duration.ofMinutes(6));
            Comment oldComment = Comment.rehydrate(id, EPISODE_ID, AUTHOR, BODY, oldSubmittedAt);
            repository.save(oldComment);

            assertThrows(EditWindowExpired.class,
                () -> service.edit(new EditCommentCommand(id, new CommentBody("Too late."))));
        }
    }
}
