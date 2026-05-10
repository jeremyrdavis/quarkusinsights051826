package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentRepository;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link CommentRepositoryImpl} against a real Postgres
 * (Dev Services). Each test runs in its own transaction that is rolled back on
 * completion via {@link TestTransaction}.
 */
@QuarkusTest
class CommentRepositoryImplTest {

    @Inject
    CommentRepository repository;

    @Test
    @TestTransaction
    void saveAndFindById() {
        Comment comment = Comment.submit(
            EpisodeId.random(),
            new AuthorHandle("testauthor"),
            new CommentBody("Great episode!")
        );
        comment.clearRecordedEvents();

        repository.save(comment);

        Optional<Comment> loaded = repository.findById(comment.id());
        assertTrue(loaded.isPresent());
        assertEquals(comment.id(), loaded.get().id());
        assertEquals(comment.body(), loaded.get().body());
        assertEquals(comment.author(), loaded.get().author());
    }

    @Test
    @TestTransaction
    void findByIdReturnsEmptyWhenAbsent() {
        Optional<Comment> loaded = repository.findById(CommentId.random());
        assertTrue(loaded.isEmpty());
    }

    @Test
    @TestTransaction
    void findByEpisodeReturnsChronological() {
        EpisodeId episodeId = EpisodeId.random();
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        // Rehydrate with explicit timestamps to control ordering
        Comment first = Comment.rehydrate(
            CommentId.random(), episodeId,
            new AuthorHandle("firstuser"),
            new CommentBody("First comment"),
            base.minusSeconds(120)
        );
        Comment second = Comment.rehydrate(
            CommentId.random(), episodeId,
            new AuthorHandle("seconduser"),
            new CommentBody("Second comment"),
            base.minusSeconds(60)
        );
        Comment third = Comment.rehydrate(
            CommentId.random(), episodeId,
            new AuthorHandle("thirduser"),
            new CommentBody("Third comment"),
            base
        );

        // Save in reverse order to verify ORDER BY works, not insertion order
        repository.save(third);
        repository.save(first);
        repository.save(second);

        List<Comment> results = repository.findByEpisode(episodeId);
        assertEquals(3, results.size());
        assertEquals(first.id(), results.get(0).id());
        assertEquals(second.id(), results.get(1).id());
        assertEquals(third.id(), results.get(2).id());
    }

    @Test
    @TestTransaction
    void findByEpisodeReturnsOnlyMatchingEpisode() {
        EpisodeId episodeA = EpisodeId.random();
        EpisodeId episodeB = EpisodeId.random();

        Comment commentA = Comment.submit(episodeA, new AuthorHandle("userA"), new CommentBody("For episode A"));
        Comment commentB = Comment.submit(episodeB, new AuthorHandle("userB"), new CommentBody("For episode B"));
        commentA.clearRecordedEvents();
        commentB.clearRecordedEvents();

        repository.save(commentA);
        repository.save(commentB);

        List<Comment> resultsA = repository.findByEpisode(episodeA);
        assertEquals(1, resultsA.size());
        assertEquals(commentA.id(), resultsA.get(0).id());
    }

    @Test
    @TestTransaction
    void saveUpdatesExistingComment() {
        // Submit a comment with current time (edit window is open)
        Comment comment = Comment.submit(
            EpisodeId.random(),
            new AuthorHandle("editauthor"),
            new CommentBody("Original body")
        );
        comment.clearRecordedEvents();
        repository.save(comment);

        // Load, edit (within window since submittedAt is now), and save again
        Comment loaded = repository.findById(comment.id()).orElseThrow();
        loaded.edit(new CommentBody("Updated body text"));
        loaded.clearRecordedEvents();
        repository.save(loaded);

        Comment reloaded = repository.findById(comment.id()).orElseThrow();
        assertEquals(comment.id(), reloaded.id());
        assertEquals(new CommentBody("Updated body text"), reloaded.body());
    }
}
