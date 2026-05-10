package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AlreadyRated;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingRepository;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link RatingRepositoryImpl} against a real Postgres
 * (Dev Services). Each test runs in its own transaction that is rolled back on
 * completion via {@link TestTransaction}.
 *
 * <p>The critical test is {@link #saveThrowsAlreadyRatedOnDuplicateConstraint}
 * which validates the §3.6 backstop: when a duplicate (episodeId, authorHandle)
 * is attempted, the repository catches the UNIQUE constraint violation and
 * re-throws {@link AlreadyRated} with the first rating's id in
 * {@code existingRatingId()}.
 */
@QuarkusTest
class RatingRepositoryImplTest {

    @Inject
    RatingRepository repository;

    @Test
    @TestTransaction
    void saveAndFindById() {
        Rating rating = Rating.submit(
            EpisodeId.random(),
            new AuthorHandle("ratinguser"),
            new Stars(4)
        );
        rating.clearRecordedEvents();

        repository.save(rating);

        Optional<Rating> loaded = repository.findById(rating.id());
        assertTrue(loaded.isPresent());
        assertEquals(rating.id(), loaded.get().id());
        assertEquals(rating.stars(), loaded.get().stars());
        assertEquals(rating.author(), loaded.get().author());
    }

    @Test
    @TestTransaction
    void findByIdReturnsEmptyWhenAbsent() {
        Optional<Rating> loaded = repository.findById(RatingId.random());
        assertTrue(loaded.isEmpty());
    }

    @Test
    @TestTransaction
    void findByEpisodeAndAuthorLocates() {
        EpisodeId episodeId = EpisodeId.random();
        AuthorHandle author = new AuthorHandle("searchme");

        Rating rating = Rating.submit(episodeId, author, new Stars(3));
        rating.clearRecordedEvents();
        repository.save(rating);

        Optional<Rating> found = repository.findByEpisodeAndAuthor(episodeId, author);
        assertTrue(found.isPresent());
        assertEquals(rating.id(), found.get().id());
    }

    @Test
    @TestTransaction
    void findByEpisodeAndAuthorReturnsEmptyForDifferentAuthor() {
        EpisodeId episodeId = EpisodeId.random();
        Rating rating = Rating.submit(episodeId, new AuthorHandle("someauthor"), new Stars(2));
        rating.clearRecordedEvents();
        repository.save(rating);

        Optional<Rating> found = repository.findByEpisodeAndAuthor(episodeId, new AuthorHandle("otherauthor"));
        assertTrue(found.isEmpty());
    }

    @Test
    @TestTransaction
    void findByEpisodeReturnsAll() {
        EpisodeId episodeId = EpisodeId.random();

        Rating r1 = Rating.submit(episodeId, new AuthorHandle("user1xx"), new Stars(5));
        Rating r2 = Rating.submit(episodeId, new AuthorHandle("user2xx"), new Stars(3));
        Rating r3 = Rating.submit(episodeId, new AuthorHandle("user3xx"), new Stars(1));
        r1.clearRecordedEvents();
        r2.clearRecordedEvents();
        r3.clearRecordedEvents();

        repository.save(r1);
        repository.save(r2);
        repository.save(r3);

        List<Rating> results = repository.findByEpisode(episodeId);
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(r -> r.id().equals(r1.id())));
        assertTrue(results.stream().anyMatch(r -> r.id().equals(r2.id())));
        assertTrue(results.stream().anyMatch(r -> r.id().equals(r3.id())));
    }

    /**
     * CRITICAL §3.6 BACKSTOP TEST: verifies that the third enforcement place
     * works correctly. When a duplicate (episodeId, authorHandle) is inserted,
     * the UNIQUE constraint {@code uk_rating_episode_author} fires and the
     * repository translates it to {@link AlreadyRated} carrying the first
     * rating's id in {@code existingRatingId()}.
     */
    @Test
    @TestTransaction
    void saveThrowsAlreadyRatedOnDuplicateConstraint() {
        EpisodeId episodeId = EpisodeId.random();
        AuthorHandle author = new AuthorHandle("duplicator");

        // First save succeeds
        Rating first = Rating.submit(episodeId, author, new Stars(5));
        first.clearRecordedEvents();
        repository.save(first);

        // Second save with same (episodeId, author) but different RatingId must throw
        Rating duplicate = Rating.submit(episodeId, author, new Stars(3));
        duplicate.clearRecordedEvents();

        AlreadyRated ex = assertThrows(AlreadyRated.class, () -> repository.save(duplicate));

        // The exception must carry the FIRST rating's id, not the duplicate's
        assertEquals(first.id(), ex.existingRatingId(),
            "existingRatingId should reference the pre-existing (first) Rating");
        assertEquals(episodeId, ex.episodeId());
        assertEquals(author, ex.author());
    }
}
