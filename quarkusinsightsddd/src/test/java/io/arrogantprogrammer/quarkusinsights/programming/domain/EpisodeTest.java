package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link Episode} aggregate behavior: lifecycle invariants,
 * event recording, idempotent assignment, and rehydration semantics.
 *
 * <p>Tests are grouped by behavior method using {@link Nested} classes.
 */
class EpisodeTest {

    private static final EpisodeNumber numberOne = new EpisodeNumber(1);
    private static final EpisodeTitle titlePilot = new EpisodeTitle("Pilot");
    private static final AirDate today = new AirDate(LocalDate.now());
    private static final AirDate yesterday = new AirDate(LocalDate.now().minusDays(1));
    private static final AirDate tomorrow = new AirDate(LocalDate.now().plusDays(1));

    @Nested
    class Schedule {

        @Test
        void factorySetsScheduledStatus() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertEquals(EpisodeStatus.SCHEDULED, episode.status());
        }

        @Test
        void factoryAssignsId() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertNotNull(episode.id());
        }

        @Test
        void factoryStoresNumberTitleAirDate() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertEquals(numberOne, episode.number());
            assertEquals(titlePilot, episode.title());
            assertEquals(tomorrow, episode.airDate());
        }

        @Test
        void factoryStartsWithoutAbstract() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertNull(episode.theAbstract());
        }

        @Test
        void factoryStartsWithEmptyPresentersAndSpeakers() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertTrue(episode.presenters().isEmpty());
            assertTrue(episode.speakers().isEmpty());
        }

        @Test
        void factoryRecordsEpisodeScheduledEvent() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            List<DomainEvent> events = episode.recordedEvents();
            assertEquals(1, events.size());
            EpisodeScheduled e = assertInstanceOf(EpisodeScheduled.class, events.get(0));
            assertEquals(episode.id(), e.episodeId());
            assertEquals(numberOne, e.number());
            assertEquals(tomorrow, e.airDate());
            assertNotNull(e.occurredAt());
        }

        @Test
        void factoryAcceptsAirDateToday() {
            Episode episode = Episode.schedule(numberOne, titlePilot, today);
            assertEquals(EpisodeStatus.SCHEDULED, episode.status());
        }

        @Test
        void factoryRejectsAirDateInPast() {
            assertThrows(AirDateInPast.class,
                () -> Episode.schedule(numberOne, titlePilot, yesterday));
        }

        @Test
        void clearRecordedEventsEmptiesTheList() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertEquals(1, episode.recordedEvents().size());
            episode.clearRecordedEvents();
            assertTrue(episode.recordedEvents().isEmpty());
        }

        @Test
        void recordedEventsReturnsCopy() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            List<DomainEvent> snapshot = episode.recordedEvents();
            assertThrows(UnsupportedOperationException.class, () -> snapshot.add(null));
        }
    }
}
