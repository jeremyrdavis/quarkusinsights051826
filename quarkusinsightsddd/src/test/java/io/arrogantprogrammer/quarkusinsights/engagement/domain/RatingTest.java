package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Rating} aggregate root.
 */
class RatingTest {

    private static final EpisodeId EPISODE_ID = EpisodeId.random();
    private static final AuthorHandle AUTHOR = new AuthorHandle("bob42");
    private static final Stars STARS = new Stars(4);

    @Nested
    class Submit {

        @Test
        void setsAllFields() {
            Rating rating = Rating.submit(EPISODE_ID, AUTHOR, STARS);
            assertNotNull(rating.id());
            assertEquals(EPISODE_ID, rating.episodeId());
            assertEquals(AUTHOR, rating.author());
            assertEquals(STARS, rating.stars());
            assertNotNull(rating.submittedAt());
        }

        @Test
        void generatesUniqueId() {
            Rating r1 = Rating.submit(EPISODE_ID, AUTHOR, STARS);
            Rating r2 = Rating.submit(EPISODE_ID, AUTHOR, STARS);
            assertTrue(!r1.id().equals(r2.id()));
        }

        @Test
        void recordsRatingSubmittedEvent() {
            Rating rating = Rating.submit(EPISODE_ID, AUTHOR, STARS);
            List<?> events = rating.recordedEvents();
            assertEquals(1, events.size());
            RatingSubmitted event = assertInstanceOf(RatingSubmitted.class, events.get(0));
            assertEquals(rating.id(), event.ratingId());
            assertEquals(EPISODE_ID, event.episodeId());
            assertEquals(AUTHOR, event.author());
            assertEquals(STARS, event.stars());
            assertNotNull(event.occurredAt());
        }
    }

    @Nested
    class Rehydrate {

        @Test
        void preservesAllState() {
            RatingId id = RatingId.random();
            Instant submitted = Instant.now().minusSeconds(30);
            Rating rating = Rating.rehydrate(id, EPISODE_ID, AUTHOR, STARS, submitted);

            assertEquals(id, rating.id());
            assertEquals(EPISODE_ID, rating.episodeId());
            assertEquals(AUTHOR, rating.author());
            assertEquals(STARS, rating.stars());
            assertEquals(submitted, rating.submittedAt());
        }

        @Test
        void noEventsRecorded() {
            Rating rating = Rating.rehydrate(
                RatingId.random(), EPISODE_ID, AUTHOR, STARS, Instant.now());
            assertTrue(rating.recordedEvents().isEmpty());
        }
    }

    @Nested
    class Immutable {

        @Test
        void noPublicMutatorMethodsExist() {
            Class<?> klass = Rating.class;
            long mutatorCount = Stream.of(klass.getMethods())
                .filter(m -> m.getName().startsWith("set")
                    || m.getName().startsWith("update")
                    || m.getName().startsWith("change"))
                .count();
            assertEquals(0, mutatorCount,
                "Rating must not expose any public set/update/change methods");
        }
    }
}
