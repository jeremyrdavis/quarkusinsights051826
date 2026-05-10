package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Comment} aggregate root.
 */
class CommentTest {

    private static final EpisodeId EPISODE_ID = EpisodeId.random();
    private static final AuthorHandle AUTHOR = new AuthorHandle("alice");
    private static final CommentBody BODY = new CommentBody("Great episode!");

    @Nested
    class Submit {

        @Test
        void setsAllFields() {
            Comment comment = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            assertNotNull(comment.id());
            assertEquals(EPISODE_ID, comment.episodeId());
            assertEquals(AUTHOR, comment.author());
            assertEquals(BODY, comment.body());
            assertNotNull(comment.submittedAt());
        }

        @Test
        void generatesUniqueId() {
            Comment c1 = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            Comment c2 = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            assertNotNull(c1.id());
            assertNotNull(c2.id());
            // Two different submissions should produce two different ids
            // (probability of collision is negligible with UUID v4)
            assertTrue(!c1.id().equals(c2.id()));
        }

        @Test
        void recordsCommentSubmittedEvent() {
            Comment comment = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            List<?> events = comment.recordedEvents();
            assertEquals(1, events.size());
            CommentSubmitted event = assertInstanceOf(CommentSubmitted.class, events.get(0));
            assertEquals(comment.id(), event.commentId());
            assertEquals(EPISODE_ID, event.episodeId());
            assertEquals(AUTHOR, event.author());
            assertNotNull(event.occurredAt());
        }
    }

    @Nested
    class Edit {

        @Test
        void withinWindowUpdatesBodyAndRecordsEvent() {
            Comment comment = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            comment.clearRecordedEvents();

            CommentBody newBody = new CommentBody("Updated comment body.");
            comment.edit(newBody);

            assertEquals(newBody, comment.body());
            List<?> events = comment.recordedEvents();
            assertEquals(1, events.size());
            CommentEdited event = assertInstanceOf(CommentEdited.class, events.get(0));
            assertEquals(comment.id(), event.commentId());
            assertNotNull(event.occurredAt());
        }

        @Test
        void outsideWindowThrowsEditWindowExpired() {
            // Rehydrate with a submittedAt 6 minutes in the past to force expiration
            CommentId id = CommentId.random();
            Instant oldSubmittedAt = Instant.now().minus(Duration.ofMinutes(6));
            Comment comment = Comment.rehydrate(id, EPISODE_ID, AUTHOR, BODY, oldSubmittedAt);

            EditWindowExpired ex = assertThrows(EditWindowExpired.class,
                () -> comment.edit(new CommentBody("Too late.")));
            assertEquals(id, ex.commentId());
            assertEquals(oldSubmittedAt, ex.submittedAt());
        }

        @Test
        void rejectsNullBody() {
            Comment comment = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            assertThrows(IllegalArgumentException.class, () -> comment.edit(null));
        }
    }

    @Nested
    class Rehydrate {

        @Test
        void preservesAllState() {
            CommentId id = CommentId.random();
            Instant submitted = Instant.now().minusSeconds(30);
            Comment comment = Comment.rehydrate(id, EPISODE_ID, AUTHOR, BODY, submitted);

            assertEquals(id, comment.id());
            assertEquals(EPISODE_ID, comment.episodeId());
            assertEquals(AUTHOR, comment.author());
            assertEquals(BODY, comment.body());
            assertEquals(submitted, comment.submittedAt());
        }

        @Test
        void noEventsRecorded() {
            Comment comment = Comment.rehydrate(
                CommentId.random(), EPISODE_ID, AUTHOR, BODY, Instant.now());
            assertTrue(comment.recordedEvents().isEmpty());
        }
    }

    @Nested
    class RecordedEvents {

        @Test
        void returnsUnmodifiableCopy() {
            Comment comment = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            List<?> events = comment.recordedEvents();
            assertThrows(UnsupportedOperationException.class,
                () -> ((List<Object>) events).add(null));
        }

        @Test
        void clearRecordedEventsEmptiesList() {
            Comment comment = Comment.submit(EPISODE_ID, AUTHOR, BODY);
            assertEquals(1, comment.recordedEvents().size());
            comment.clearRecordedEvents();
            assertTrue(comment.recordedEvents().isEmpty());
        }
    }
}
