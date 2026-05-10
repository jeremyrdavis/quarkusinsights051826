package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    /**
     * Builds an Episode in {@link EpisodeStatus#LIVE} with one abstract,
     * one presenter, and one speaker — the precondition state for
     * {@link Episode#publish()}. Used by SubmitAbstract,
     * AssignPresenter, AssignSpeaker, GoLive, Publish, Cancel tests.
     */
    private static Episode liveEpisodeReadyToPublish() {
        Episode episode = Episode.schedule(numberOne, titlePilot, today);
        episode.submitAbstract(new AbstractText("a".repeat(150)));
        episode.assignPresenter(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random());
        episode.assignSpeaker(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random());
        episode.goLive();
        return episode;
    }

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

    @Nested
    class SubmitAbstract {

        private static final AbstractText sample =
            new AbstractText("This is the abstract for the pilot episode. ".repeat(3));

        @Test
        void setsTheAbstract() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.submitAbstract(sample);
            assertNotNull(episode.theAbstract());
            assertEquals(sample, episode.theAbstract().text());
        }

        @Test
        void recordsAbstractSubmittedEvent() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.clearRecordedEvents();
            episode.submitAbstract(sample);
            List<DomainEvent> events = episode.recordedEvents();
            assertEquals(1, events.size());
            AbstractSubmitted e = assertInstanceOf(AbstractSubmitted.class, events.get(0));
            assertEquals(episode.id(), e.episodeId());
            assertEquals(episode.theAbstract().id(), e.abstractId());
        }

        @Test
        void replacesAnExistingAbstract() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.submitAbstract(sample);
            AbstractId firstId = episode.theAbstract().id();
            AbstractText replacement =
                new AbstractText("A different abstract text. ".repeat(5));
            episode.submitAbstract(replacement);
            assertEquals(replacement, episode.theAbstract().text());
            assertNotEquals(firstId, episode.theAbstract().id());
        }

        @Test
        void rejectsNullText() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertThrows(IllegalArgumentException.class, () -> episode.submitAbstract(null));
        }

        @Test
        void rejectsCallWhenLive() {
            Episode episode = Episode.schedule(numberOne, titlePilot, today);
            episode.submitAbstract(sample);
            episode.assignPresenter(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random());
            episode.assignSpeaker(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random());
            episode.goLive();
            assertThrows(IllegalEpisodeTransition.class, () -> episode.submitAbstract(sample));
        }

        @Test
        void rejectsCallWhenPublished() {
            Episode episode = liveEpisodeReadyToPublish();
            episode.publish();
            assertThrows(IllegalEpisodeTransition.class, () -> episode.submitAbstract(sample));
        }

        @Test
        void rejectsCallWhenCanceled() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.cancel("Scheduling conflict");
            assertThrows(IllegalEpisodeTransition.class, () -> episode.submitAbstract(sample));
        }
    }

    @Nested
    class AssignPresenter {

        @Test
        void addsThePresenter() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            io.arrogantprogrammer.quarkusinsights.shared.PersonId person =
                io.arrogantprogrammer.quarkusinsights.shared.PersonId.random();
            episode.assignPresenter(person);
            assertTrue(episode.presenters().contains(person));
        }

        @Test
        void recordsPresenterAssignedEvent() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.clearRecordedEvents();
            io.arrogantprogrammer.quarkusinsights.shared.PersonId person =
                io.arrogantprogrammer.quarkusinsights.shared.PersonId.random();
            episode.assignPresenter(person);
            List<DomainEvent> events = episode.recordedEvents();
            assertEquals(1, events.size());
            PresenterAssigned e = assertInstanceOf(PresenterAssigned.class, events.get(0));
            assertEquals(episode.id(), e.episodeId());
            assertEquals(person, e.personId());
        }

        @Test
        void isIdempotentAndDoesNotEmitDuplicateEvent() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            io.arrogantprogrammer.quarkusinsights.shared.PersonId person =
                io.arrogantprogrammer.quarkusinsights.shared.PersonId.random();
            episode.assignPresenter(person);
            episode.clearRecordedEvents();
            episode.assignPresenter(person);
            assertTrue(episode.recordedEvents().isEmpty());
            assertEquals(1, episode.presenters().size());
        }

        @Test
        void allowedWhileLive() {
            Episode episode = liveEpisodeReadyToPublish();
            episode.clearRecordedEvents();
            io.arrogantprogrammer.quarkusinsights.shared.PersonId additional =
                io.arrogantprogrammer.quarkusinsights.shared.PersonId.random();
            episode.assignPresenter(additional);
            assertTrue(episode.presenters().contains(additional));
            assertEquals(1, episode.recordedEvents().size());
        }

        @Test
        void rejectsCallWhenPublished() {
            Episode episode = liveEpisodeReadyToPublish();
            episode.publish();
            assertThrows(IllegalEpisodeTransition.class,
                () -> episode.assignPresenter(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random()));
        }

        @Test
        void rejectsCallWhenCanceled() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.cancel("any reason");
            assertThrows(IllegalEpisodeTransition.class,
                () -> episode.assignPresenter(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random()));
        }

        @Test
        void rejectsNullPersonId() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertThrows(IllegalArgumentException.class, () -> episode.assignPresenter(null));
        }
    }

    @Nested
    class AssignSpeaker {

        @Test
        void addsTheSpeaker() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            io.arrogantprogrammer.quarkusinsights.shared.PersonId person =
                io.arrogantprogrammer.quarkusinsights.shared.PersonId.random();
            episode.assignSpeaker(person);
            assertTrue(episode.speakers().contains(person));
        }

        @Test
        void recordsSpeakerAssignedEvent() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.clearRecordedEvents();
            io.arrogantprogrammer.quarkusinsights.shared.PersonId person =
                io.arrogantprogrammer.quarkusinsights.shared.PersonId.random();
            episode.assignSpeaker(person);
            List<DomainEvent> events = episode.recordedEvents();
            assertEquals(1, events.size());
            SpeakerAssigned e = assertInstanceOf(SpeakerAssigned.class, events.get(0));
            assertEquals(episode.id(), e.episodeId());
            assertEquals(person, e.personId());
        }

        @Test
        void isIdempotentAndDoesNotEmitDuplicateEvent() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            io.arrogantprogrammer.quarkusinsights.shared.PersonId person =
                io.arrogantprogrammer.quarkusinsights.shared.PersonId.random();
            episode.assignSpeaker(person);
            episode.clearRecordedEvents();
            episode.assignSpeaker(person);
            assertTrue(episode.recordedEvents().isEmpty());
            assertEquals(1, episode.speakers().size());
        }

        @Test
        void rejectsCallWhenPublished() {
            Episode episode = liveEpisodeReadyToPublish();
            episode.publish();
            assertThrows(IllegalEpisodeTransition.class,
                () -> episode.assignSpeaker(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random()));
        }

        @Test
        void rejectsCallWhenCanceled() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            episode.cancel("any reason");
            assertThrows(IllegalEpisodeTransition.class,
                () -> episode.assignSpeaker(io.arrogantprogrammer.quarkusinsights.shared.PersonId.random()));
        }

        @Test
        void rejectsNullPersonId() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertThrows(IllegalArgumentException.class, () -> episode.assignSpeaker(null));
        }
    }
}
