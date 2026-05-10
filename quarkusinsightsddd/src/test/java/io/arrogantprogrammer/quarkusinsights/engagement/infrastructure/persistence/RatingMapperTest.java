package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link RatingMapper} round-trips a Rating aggregate through
 * {@link RatingEntity} preserving all state. Pure JUnit; no Quarkus startup
 * since the mapper has no CDI dependencies of its own.
 *
 * <p>Note: no {@code applyTo} test because {@link Rating} is immutable — there
 * is no update path and no such method on {@link RatingMapper}.
 */
class RatingMapperTest {

    private RatingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RatingMapper();
    }

    @Test
    void roundTripPreservesAllFields() {
        Rating original = Rating.rehydrate(
            RatingId.random(),
            EpisodeId.random(),
            new AuthorHandle("staruser"),
            new Stars(4),
            Instant.now()
        );

        RatingEntity entity = mapper.toEntity(original);
        Rating roundTripped = mapper.toDomain(entity);

        assertEquals(original.id(), roundTripped.id());
        assertEquals(original.episodeId(), roundTripped.episodeId());
        assertEquals(original.author(), roundTripped.author());
        assertEquals(original.stars(), roundTripped.stars());
        assertEquals(original.submittedAt(), roundTripped.submittedAt());
    }

    @Test
    void toEntityMapsAllColumns() {
        RatingId id = RatingId.random();
        EpisodeId episodeId = EpisodeId.random();
        AuthorHandle author = new AuthorHandle("myhandle");
        Stars stars = new Stars(5);
        Instant submittedAt = Instant.parse("2026-02-20T14:30:00Z");

        Rating rating = Rating.rehydrate(id, episodeId, author, stars, submittedAt);
        RatingEntity entity = mapper.toEntity(rating);

        assertEquals(id.value(), entity.id);
        assertEquals(episodeId.value(), entity.episodeId);
        assertEquals(author.value(), entity.authorHandle);
        assertEquals(stars.value(), entity.stars);
        assertEquals(submittedAt, entity.submittedAt);
    }

    @Test
    void roundTripPreservesAllStarValues() {
        for (int s = 1; s <= 5; s++) {
            Rating rating = Rating.rehydrate(
                RatingId.random(),
                EpisodeId.random(),
                new AuthorHandle("user" + s),
                new Stars(s),
                Instant.now()
            );
            Rating roundTripped = mapper.toDomain(mapper.toEntity(rating));
            assertEquals(s, roundTripped.stars().value());
        }
    }
}
