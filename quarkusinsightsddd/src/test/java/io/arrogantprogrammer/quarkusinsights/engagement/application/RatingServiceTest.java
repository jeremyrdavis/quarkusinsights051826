package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AlreadyRated;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingSubmitted;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import io.arrogantprogrammer.quarkusinsights.shared.application.RecordingDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link RatingService} behavior using stub implementations
 * of the {@code RatingRepository} and {@code DomainEventPublisher}
 * ports — no Quarkus startup needed.
 *
 * <p>The {@code Submit} nested class is the critical section that covers
 * the §3.6 precheck path exhaustively: same (episode, author) is rejected;
 * same author on different episodes is allowed; different authors on same
 * episode are allowed.
 */
class RatingServiceTest {

    private static final EpisodeId EPISODE_ID = EpisodeId.random();
    private static final AuthorHandle AUTHOR = new AuthorHandle("bob42");
    private static final Stars STARS = new Stars(4);

    private InMemoryRatingRepository repository;
    private RecordingDomainEventPublisher publisher;
    private RatingService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRatingRepository();
        publisher = new RecordingDomainEventPublisher();
        service = new RatingService(repository, publisher);
    }

    @Nested
    class Submit {

        @Test
        void persistsAndReturnsId() {
            SubmitRatingCommand cmd = new SubmitRatingCommand(EPISODE_ID, AUTHOR, STARS);
            RatingId id = service.submit(cmd);
            assertNotNull(id);
            assertEquals(1, repository.size());
            Rating persisted = repository.findById(id).orElseThrow();
            assertEquals(EPISODE_ID, persisted.episodeId());
            assertEquals(AUTHOR, persisted.author());
            assertEquals(STARS, persisted.stars());
        }

        @Test
        void publishesRatingSubmittedEvent() {
            service.submit(new SubmitRatingCommand(EPISODE_ID, AUTHOR, STARS));
            assertEquals(1, publisher.publishedEvents().size());
            RatingSubmitted event = assertInstanceOf(RatingSubmitted.class,
                publisher.publishedEvents().get(0));
            assertEquals(EPISODE_ID, event.episodeId());
            assertEquals(AUTHOR, event.author());
            assertEquals(STARS, event.stars());
        }

        @Test
        void throwsAlreadyRatedWhenSameAuthorRatedSameEpisode() {
            // Pre-populate repository with an existing Rating for this (episode, author)
            RatingId existingId = RatingId.random();
            Rating existing = Rating.rehydrate(existingId, EPISODE_ID, AUTHOR, STARS, Instant.now());
            repository.save(existing);

            // Attempt to rate the same episode with the same author
            AlreadyRated ex = assertThrows(AlreadyRated.class,
                () -> service.submit(new SubmitRatingCommand(EPISODE_ID, AUTHOR, new Stars(3))));

            assertEquals(existingId, ex.existingRatingId(),
                "AlreadyRated must carry the pre-existing rating's id");
            assertEquals(EPISODE_ID, ex.episodeId());
            assertEquals(AUTHOR, ex.author());
            // The duplicate was not saved
            assertEquals(1, repository.size());
        }

        @Test
        void allowsSameAuthorToRateDifferentEpisodes() {
            EpisodeId otherEpisode = EpisodeId.random();

            service.submit(new SubmitRatingCommand(EPISODE_ID, AUTHOR, STARS));
            service.submit(new SubmitRatingCommand(otherEpisode, AUTHOR, new Stars(5)));

            assertEquals(2, repository.size());
        }

        @Test
        void allowsDifferentAuthorsOnSameEpisode() {
            AuthorHandle otherAuthor = new AuthorHandle("carol99");

            service.submit(new SubmitRatingCommand(EPISODE_ID, AUTHOR, STARS));
            service.submit(new SubmitRatingCommand(EPISODE_ID, otherAuthor, new Stars(2)));

            assertEquals(2, repository.size());
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            RatingId id = service.submit(new SubmitRatingCommand(EPISODE_ID, AUTHOR, STARS));
            Rating persisted = repository.findById(id).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }
}
