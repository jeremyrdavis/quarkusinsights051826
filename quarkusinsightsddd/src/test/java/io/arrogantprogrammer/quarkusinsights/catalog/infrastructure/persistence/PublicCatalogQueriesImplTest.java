package io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.catalog.application.PublicCatalogQueries;
import io.arrogantprogrammer.quarkusinsights.catalog.domain.PublicEpisodeView;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link PublicCatalogQueriesImpl} against a real
 * Postgres (Dev Services). Each test runs in its own rolled-back transaction
 * via {@link TestTransaction}.
 */
@QuarkusTest
class PublicCatalogQueriesImplTest {

    @Inject
    PublicCatalogQueries queries;

    // -------------------------------------------------------------------------
    // findUpcoming
    // -------------------------------------------------------------------------

    @Test
    @TestTransaction
    void findUpcomingReturnsMostRecentScheduled() {
        // Seed three SCHEDULED episodes at different air dates
        LocalDate soon = LocalDate.now().plusDays(1);
        LocalDate sooner = LocalDate.now().plusDays(3);
        LocalDate later = LocalDate.now().plusDays(7);

        PublicEpisodeViewEntity e1 = scheduledEntity(100, "Upcoming A", sooner);
        PublicEpisodeViewEntity e2 = scheduledEntity(101, "Upcoming B", soon);  // earliest
        PublicEpisodeViewEntity e3 = scheduledEntity(102, "Upcoming C", later);
        e1.persist();
        e2.persist();
        e3.persist();

        Optional<PublicEpisodeView> result = queries.findUpcoming();

        assertTrue(result.isPresent(), "Expected an upcoming episode");
        assertEquals(101, result.get().number().value(),
            "Expected the earliest air-date episode (number 101) to be returned first");
    }

    @Test
    @TestTransaction
    void findUpcomingReturnsEmptyWhenNoScheduled() {
        // Drop any SCHEDULED rows that other test classes may have left
        // committed (e.g., the admin-controller tests POST through the
        // service layer without @TestTransaction, so their projector
        // upserts persist beyond their own test). The delete happens
        // inside this @TestTransaction and is rolled back at the end,
        // so other suites' assumptions are unaffected.
        PublicEpisodeViewEntity.delete("status", EpisodeStatus.SCHEDULED);

        // Only persist a PUBLISHED episode; no SCHEDULED ones
        PublicEpisodeViewEntity published = scheduledEntity(200, "Past Episode", LocalDate.now().minusDays(1));
        published.status = EpisodeStatus.PUBLISHED;
        published.persist();

        Optional<PublicEpisodeView> result = queries.findUpcoming();

        assertTrue(result.isEmpty(), "Expected empty when no SCHEDULED episodes exist");
    }

    // -------------------------------------------------------------------------
    // findHistory
    // -------------------------------------------------------------------------

    @Test
    @TestTransaction
    void findHistoryReturnsPublishedReverseChronological() {
        LocalDate older = LocalDate.now().minusDays(14);
        LocalDate newer = LocalDate.now().minusDays(7);

        PublicEpisodeViewEntity e1 = publishedEntity(300, "Older Episode", older);
        PublicEpisodeViewEntity e2 = publishedEntity(301, "Newer Episode", newer);
        e1.persist();
        e2.persist();

        List<PublicEpisodeView> results = queries.findHistory(0, 20);

        assertFalse(results.isEmpty(), "Expected at least two published episodes");
        // Verify descending air-date order in the results we seeded
        // Find our seeded episodes in the list
        Optional<PublicEpisodeView> found300 = results.stream()
            .filter(v -> v.number().value() == 300).findFirst();
        Optional<PublicEpisodeView> found301 = results.stream()
            .filter(v -> v.number().value() == 301).findFirst();

        assertTrue(found300.isPresent(), "Episode 300 not in history");
        assertTrue(found301.isPresent(), "Episode 301 not in history");

        int idx300 = results.indexOf(found300.get());
        int idx301 = results.indexOf(found301.get());
        assertTrue(idx301 < idx300, "Newer episode (301) should appear before older (300) in reverse-chrono order");
    }

    @Test
    @TestTransaction
    void findHistoryReturnsOnlyPublished() {
        PublicEpisodeViewEntity scheduled = scheduledEntity(400, "Not Yet", LocalDate.now().plusDays(1));
        PublicEpisodeViewEntity published = publishedEntity(401, "Published One", LocalDate.now().minusDays(3));
        scheduled.persist();
        published.persist();

        List<PublicEpisodeView> results = queries.findHistory(0, 20);

        boolean containsScheduled = results.stream().anyMatch(v -> v.number().value() == 400);
        boolean containsPublished = results.stream().anyMatch(v -> v.number().value() == 401);

        assertFalse(containsScheduled, "History must not include SCHEDULED episodes");
        assertTrue(containsPublished, "History must include PUBLISHED episodes");
    }

    // -------------------------------------------------------------------------
    // findByNumber
    // -------------------------------------------------------------------------

    @Test
    @TestTransaction
    void findByNumberReturnsMatchingView() {
        PublicEpisodeViewEntity entity = scheduledEntity(500, "Find By Number", LocalDate.now().plusDays(1));
        entity.persist();

        Optional<PublicEpisodeView> result = queries.findByNumber(new EpisodeNumber(500));

        assertTrue(result.isPresent(), "Expected to find episode 500");
        assertEquals(500, result.get().number().value());
        assertEquals("Find By Number", result.get().title().value());
    }

    @Test
    @TestTransaction
    void findByNumberReturnsEmptyForUnknownNumber() {
        Optional<PublicEpisodeView> result = queries.findByNumber(new EpisodeNumber(9999));
        assertTrue(result.isEmpty(), "Expected empty for non-existent episode number");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PublicEpisodeViewEntity scheduledEntity(int number, String title, LocalDate airDate) {
        PublicEpisodeViewEntity e = new PublicEpisodeViewEntity();
        e.id = UUID.randomUUID();
        e.number = number;
        e.title = title;
        e.airDate = airDate;
        e.status = EpisodeStatus.SCHEDULED;
        e.commentCount = 0;
        e.ratingCount = 0;
        e.lastUpdatedAt = Instant.now();
        return e;
    }

    private PublicEpisodeViewEntity publishedEntity(int number, String title, LocalDate airDate) {
        PublicEpisodeViewEntity e = scheduledEntity(number, title, airDate);
        e.status = EpisodeStatus.PUBLISHED;
        return e;
    }
}
