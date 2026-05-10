package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentSubmitted;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingSubmitted;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for {@link EngagementProjector} verifying that
 * {@code CommentSubmitted} and {@code RatingSubmitted} events correctly update
 * the comment count and rating running totals on
 * {@link PublicEpisodeViewEntity}.
 */
@QuarkusTest
class EngagementProjectorTest {

    @Inject
    Event<DomainEvent> domainEvents;

    // -------------------------------------------------------------------------
    // Comment count
    // -------------------------------------------------------------------------

    @Test
    void commentSubmittedIncrementsCommentCount() {
        EpisodeId episodeId = EpisodeId.random();
        UUID viewId = seedView(episodeId);

        domainEvents.fire(new CommentSubmitted(
            CommentId.random(),
            episodeId,
            new AuthorHandle("testuser"),
            Instant.now()
        ));

        PublicEpisodeViewEntity found = loadView(viewId);
        assertNotNull(found);
        assertEquals(1, found.commentCount, "commentCount should be 1 after one CommentSubmitted");

        // Fire a second comment
        domainEvents.fire(new CommentSubmitted(
            CommentId.random(),
            episodeId,
            new AuthorHandle("anotheruser"),
            Instant.now()
        ));

        found = loadView(viewId);
        assertEquals(2, found.commentCount, "commentCount should be 2 after two comments");

        deleteView(viewId);
    }

    // -------------------------------------------------------------------------
    // Rating running totals
    // -------------------------------------------------------------------------

    @Test
    void ratingSubmittedUpdatesRunningTotals() {
        EpisodeId episodeId = EpisodeId.random();
        UUID viewId = seedView(episodeId);

        // First rating: 3 stars
        domainEvents.fire(new RatingSubmitted(
            RatingId.random(),
            episodeId,
            new AuthorHandle("rater1"),
            new Stars(3),
            Instant.now()
        ));

        PublicEpisodeViewEntity found = loadView(viewId);
        assertNotNull(found);
        assertEquals(1, found.ratingCount, "ratingCount should be 1");
        assertEquals(0, BigDecimal.valueOf(3).compareTo(found.ratingSum), "ratingSum should be 3");
        assertEquals(
            new BigDecimal("3.00"),
            found.averageRating(),
            "average should be 3.00 after one 3-star rating"
        );

        // Second rating: 5 stars → average = (3+5)/2 = 4.00
        domainEvents.fire(new RatingSubmitted(
            RatingId.random(),
            episodeId,
            new AuthorHandle("rater2"),
            new Stars(5),
            Instant.now()
        ));

        found = loadView(viewId);
        assertEquals(2, found.ratingCount, "ratingCount should be 2");
        assertEquals(
            new BigDecimal("4.00"),
            found.averageRating(),
            "average should be 4.00 after 3-star and 5-star ratings"
        );

        deleteView(viewId);
    }

    @Test
    void commentOnUnknownEpisodeIsIgnored() {
        EpisodeId unknownId = EpisodeId.random();
        // No view seeded for this episodeId — projector should be a no-op

        domainEvents.fire(new CommentSubmitted(
            CommentId.random(),
            unknownId,
            new AuthorHandle("ghost"),
            Instant.now()
        ));
        // No exception expected; the projector silently skips missing views
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Transactional
    UUID seedView(EpisodeId episodeId) {
        PublicEpisodeViewEntity entity = new PublicEpisodeViewEntity();
        entity.id = episodeId.value();
        entity.number = 900 + (int) (Math.random() * 99);
        entity.title = "Engagement Projector Test";
        entity.airDate = LocalDate.now().minusDays(1);
        entity.status = EpisodeStatus.PUBLISHED;
        entity.commentCount = 0;
        entity.ratingCount = 0;
        entity.lastUpdatedAt = Instant.now();
        entity.persist();
        return entity.id;
    }

    @Transactional
    PublicEpisodeViewEntity loadView(UUID id) {
        return PublicEpisodeViewEntity.findById(id);
    }

    @Transactional
    void deleteView(UUID id) {
        PublicEpisodeViewEntity entity = PublicEpisodeViewEntity.findById(id);
        if (entity != null) entity.delete();
    }
}
