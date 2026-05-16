package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractSubmitted;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateNotYetReached;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeCanceled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumberAlreadyExists;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodePublished;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeScheduled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeWentLive;
import io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition;
import io.arrogantprogrammer.quarkusinsights.programming.domain.PresenterAssigned;
import io.arrogantprogrammer.quarkusinsights.programming.domain.SpeakerAssigned;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.arrogantprogrammer.quarkusinsights.shared.application.RecordingDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link EpisodeService} behavior using stub implementations
 * of the {@code EpisodeRepository} and {@code DomainEventPublisher}
 * ports — no Quarkus startup needed.
 *
 * <p>Tests are grouped by service method via {@link Nested} classes.
 * Each group corresponds to what was previously a separate
 * {@code *UseCaseTest} class; coverage is preserved 1:1.
 */
class EpisodeServiceTest {

    private InMemoryEpisodeRepository repository;
    private RecordingDomainEventPublisher publisher;
    private EpisodeService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        service = new EpisodeService(repository, publisher);
    }

    private Episode seedScheduledEpisode(int number) {
        Episode episode = Episode.schedule(
            new EpisodeNumber(number),
            new EpisodeTitle("Pilot"),
            new AirDate(LocalDate.now().plusDays(7))
        );
        episode.clearRecordedEvents();
        repository.save(episode);
        return episode;
    }

    private Episode seedLiveEpisodeReadyToPublish(int number) {
        Episode episode = Episode.schedule(
            new EpisodeNumber(number),
            new EpisodeTitle("Pilot"),
            new AirDate(LocalDate.now())
        );
        episode.submitAbstract(new AbstractText("a".repeat(150)));
        episode.assignPresenter(PersonId.random());
        episode.assignSpeaker(PersonId.random());
        episode.goLive();
        episode.clearRecordedEvents();
        repository.save(episode);
        return episode;
    }

    @Nested
    class Schedule {

        @Test
        void persistsAndReturnsId() {
            ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
                new EpisodeNumber(1),
                new EpisodeTitle("Pilot"),
                new AirDate(LocalDate.now().plusDays(7))
            );
            EpisodeId id = service.schedule(cmd);
            assertNotNull(id);
            assertEquals(1, repository.size());
            Episode persisted = repository.findById(id).orElseThrow();
            assertEquals(EpisodeStatus.SCHEDULED, persisted.status());
        }

        @Test
        void publishesEpisodeScheduledEvent() {
            service.schedule(new ScheduleEpisodeCommand(
                new EpisodeNumber(2),
                new EpisodeTitle("E2"),
                new AirDate(LocalDate.now().plusDays(14))
            ));
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(EpisodeScheduled.class, publisher.publishedEvents().get(0));
        }

        @Test
        void throwsWhenNumberAlreadyExists() {
            EpisodeNumber duplicate = new EpisodeNumber(7);
            Episode existing = Episode.schedule(duplicate,
                new EpisodeTitle("Already there"),
                new AirDate(LocalDate.now().plusDays(1)));
            existing.clearRecordedEvents();
            repository.save(existing);

            EpisodeNumberAlreadyExists ex = assertThrows(EpisodeNumberAlreadyExists.class,
                () -> service.schedule(new ScheduleEpisodeCommand(
                    duplicate,
                    new EpisodeTitle("Conflict"),
                    new AirDate(LocalDate.now().plusDays(2)))));
            assertEquals(duplicate, ex.number());
            assertEquals(1, repository.size());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void propagatesAirDateInPast() {
            assertThrows(AirDateInPast.class,
                () -> service.schedule(new ScheduleEpisodeCommand(
                    new EpisodeNumber(3),
                    new EpisodeTitle("Past"),
                    new AirDate(LocalDate.now().minusDays(1)))));
            assertEquals(0, repository.size());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            EpisodeId id = service.schedule(new ScheduleEpisodeCommand(
                new EpisodeNumber(4),
                new EpisodeTitle("Clear test"),
                new AirDate(LocalDate.now().plusDays(3))));
            Episode persisted = repository.findById(id).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }

    @Nested
    class SubmitAbstract {

        private static final AbstractText sampleText = new AbstractText("a".repeat(150));

        @Test
        void attachesAbstractAndPublishesEvent() {
            Episode episode = seedScheduledEpisode(1);
            service.submitAbstract(new SubmitAbstractCommand(episode.id(), sampleText));
            Episode persisted = repository.findById(episode.id()).orElseThrow();
            assertEquals(sampleText, persisted.theAbstract().text());
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(AbstractSubmitted.class, publisher.publishedEvents().get(0));
        }

        @Test
        void throwsEpisodeNotFoundWhenMissing() {
            EpisodeId missing = EpisodeId.random();
            EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
                () -> service.submitAbstract(new SubmitAbstractCommand(missing, sampleText)));
            assertEquals(missing, ex.episodeId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void propagatesIllegalEpisodeTransitionWhenLive() {
            Episode episode = seedLiveEpisodeReadyToPublish(2);
            assertThrows(IllegalEpisodeTransition.class,
                () -> service.submitAbstract(new SubmitAbstractCommand(episode.id(), sampleText)));
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            Episode episode = seedScheduledEpisode(3);
            service.submitAbstract(new SubmitAbstractCommand(episode.id(), sampleText));
            Episode persisted = repository.findById(episode.id()).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }

    @Nested
    class AssignPresenter {

        @Test
        void addsAndPublishesEvent() {
            Episode episode = seedScheduledEpisode(1);
            PersonId person = PersonId.random();
            service.assignPresenter(new AssignPresenterCommand(episode.id(), person));
            Episode persisted = repository.findById(episode.id()).orElseThrow();
            assertTrue(persisted.presenters().contains(person));
            assertEquals(1, publisher.publishedEvents().size());
            PresenterAssigned event = assertInstanceOf(PresenterAssigned.class, publisher.publishedEvents().get(0));
            assertEquals(episode.id(), event.episodeId());
            assertEquals(person, event.personId());
        }

        @Test
        void duplicateAssignmentPublishesNoEvent() {
            Episode episode = seedScheduledEpisode(2);
            PersonId person = PersonId.random();
            service.assignPresenter(new AssignPresenterCommand(episode.id(), person));

            // Reset publisher to isolate the second call
            RecordingDomainEventPublisher publisher2 = new RecordingDomainEventPublisher();
            EpisodeService service2 = new EpisodeService(repository, publisher2);
            service2.assignPresenter(new AssignPresenterCommand(episode.id(), person));

            assertTrue(publisher2.publishedEvents().isEmpty());
            assertEquals(1, repository.findById(episode.id()).orElseThrow().presenters().size());
        }

        @Test
        void throwsEpisodeNotFoundWhenMissing() {
            EpisodeId missing = EpisodeId.random();
            EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
                () -> service.assignPresenter(new AssignPresenterCommand(missing, PersonId.random())));
            assertEquals(missing, ex.episodeId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void propagatesIllegalEpisodeTransitionWhenPublished() {
            Episode episode = seedLiveEpisodeReadyToPublish(3);
            service.publish(new PublishEpisodeCommand(episode.id()));
            // Reset publisher to isolate the assertion
            publisher = new RecordingDomainEventPublisher();
            service = new EpisodeService(repository, publisher);
            assertThrows(IllegalEpisodeTransition.class,
                () -> service.assignPresenter(new AssignPresenterCommand(episode.id(), PersonId.random())));
            assertTrue(publisher.publishedEvents().isEmpty());
        }
    }

    @Nested
    class AssignSpeaker {

        @Test
        void addsAndPublishesEvent() {
            Episode episode = seedScheduledEpisode(1);
            PersonId person = PersonId.random();
            service.assignSpeaker(new AssignSpeakerCommand(episode.id(), person));
            Episode persisted = repository.findById(episode.id()).orElseThrow();
            assertTrue(persisted.speakers().contains(person));
            assertEquals(1, publisher.publishedEvents().size());
            SpeakerAssigned event = assertInstanceOf(SpeakerAssigned.class, publisher.publishedEvents().get(0));
            assertEquals(episode.id(), event.episodeId());
            assertEquals(person, event.personId());
        }

        @Test
        void duplicateAssignmentPublishesNoEvent() {
            Episode episode = seedScheduledEpisode(2);
            PersonId person = PersonId.random();
            service.assignSpeaker(new AssignSpeakerCommand(episode.id(), person));

            RecordingDomainEventPublisher publisher2 = new RecordingDomainEventPublisher();
            EpisodeService service2 = new EpisodeService(repository, publisher2);
            service2.assignSpeaker(new AssignSpeakerCommand(episode.id(), person));

            assertTrue(publisher2.publishedEvents().isEmpty());
            assertEquals(1, repository.findById(episode.id()).orElseThrow().speakers().size());
        }

        @Test
        void throwsEpisodeNotFoundWhenMissing() {
            EpisodeId missing = EpisodeId.random();
            EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
                () -> service.assignSpeaker(new AssignSpeakerCommand(missing, PersonId.random())));
            assertEquals(missing, ex.episodeId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void propagatesIllegalEpisodeTransitionWhenPublished() {
            Episode episode = seedLiveEpisodeReadyToPublish(3);
            service.publish(new PublishEpisodeCommand(episode.id()));
            publisher = new RecordingDomainEventPublisher();
            service = new EpisodeService(repository, publisher);
            assertThrows(IllegalEpisodeTransition.class,
                () -> service.assignSpeaker(new AssignSpeakerCommand(episode.id(), PersonId.random())));
            assertTrue(publisher.publishedEvents().isEmpty());
        }
    }

    @Nested
    class GoLive {

        @Test
        void transitionsToLiveAndPublishesEvent() {
            Episode episode = Episode.schedule(
                new EpisodeNumber(1),
                new EpisodeTitle("Pilot"),
                new AirDate(LocalDate.now())
            );
            episode.clearRecordedEvents();
            repository.save(episode);

            service.goLive(new GoLiveCommand(episode.id()));

            Episode persisted = repository.findById(episode.id()).orElseThrow();
            assertEquals(EpisodeStatus.LIVE, persisted.status());
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(EpisodeWentLive.class, publisher.publishedEvents().get(0));
        }

        @Test
        void throwsEpisodeNotFoundWhenMissing() {
            EpisodeId missing = EpisodeId.random();
            EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
                () -> service.goLive(new GoLiveCommand(missing)));
            assertEquals(missing, ex.episodeId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void propagatesAirDateNotYetReached() {
            Episode episode = seedScheduledEpisode(2);
            assertThrows(AirDateNotYetReached.class,
                () -> service.goLive(new GoLiveCommand(episode.id())));
            assertTrue(publisher.publishedEvents().isEmpty());
        }
    }

    @Nested
    class Publish {

        @Test
        void transitionsToPublishedAndPublishesEvent() {
            Episode episode = seedLiveEpisodeReadyToPublish(1);
            service.publish(new PublishEpisodeCommand(episode.id()));
            Episode persisted = repository.findById(episode.id()).orElseThrow();
            assertEquals(EpisodeStatus.PUBLISHED, persisted.status());
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(EpisodePublished.class, publisher.publishedEvents().get(0));
        }

        @Test
        void throwsEpisodeNotFoundWhenMissing() {
            EpisodeId missing = EpisodeId.random();
            EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
                () -> service.publish(new PublishEpisodeCommand(missing)));
            assertEquals(missing, ex.episodeId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void propagatesIllegalEpisodeTransitionWhenScheduled() {
            Episode episode = seedScheduledEpisode(2);
            assertThrows(IllegalEpisodeTransition.class,
                () -> service.publish(new PublishEpisodeCommand(episode.id())));
            assertTrue(publisher.publishedEvents().isEmpty());
        }
    }

    @Nested
    class Cancel {

        @Test
        void cancelsAndPublishesEventWithReason() {
            Episode episode = seedScheduledEpisode(1);
            service.cancel(new CancelEpisodeCommand(episode.id(), "Studio booking conflict"));
            Episode persisted = repository.findById(episode.id()).orElseThrow();
            assertEquals(EpisodeStatus.CANCELED, persisted.status());
            assertEquals(1, publisher.publishedEvents().size());
            EpisodeCanceled event = assertInstanceOf(EpisodeCanceled.class, publisher.publishedEvents().get(0));
            assertEquals("Studio booking conflict", event.reason());
        }

        @Test
        void throwsEpisodeNotFoundWhenMissing() {
            EpisodeId missing = EpisodeId.random();
            EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
                () -> service.cancel(new CancelEpisodeCommand(missing, "any reason")));
            assertEquals(missing, ex.episodeId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void propagatesIllegalEpisodeTransitionWhenLive() {
            Episode episode = seedLiveEpisodeReadyToPublish(2);
            assertThrows(IllegalEpisodeTransition.class,
                () -> service.cancel(new CancelEpisodeCommand(episode.id(), "too late")));
            assertTrue(publisher.publishedEvents().isEmpty());
        }
    }

    @Nested
    class Compose {

        private ComposeEpisodeCommand validCommand(int number) {
            return new ComposeEpisodeCommand(
                new EpisodeNumber(number),
                new EpisodeTitle("Pilot"),
                new AirDate(LocalDate.now().plusDays(7)),
                new AbstractText("a".repeat(150)),
                java.util.Set.of(PersonId.random()),
                java.util.Set.of(PersonId.random())
            );
        }

        @Test
        void persistsAndReturnsIdOfNewEpisode() {
            EpisodeId id = service.compose(validCommand(1));
            assertNotNull(id);
            Episode persisted = repository.findById(id).orElseThrow();
            assertEquals(EpisodeStatus.SCHEDULED, persisted.status());
            assertNotNull(persisted.theAbstract());
            assertEquals(1, persisted.presenters().size());
            assertEquals(1, persisted.speakers().size());
        }

        @Test
        void publishesAllEventsInOrder() {
            service.compose(validCommand(1));
            java.util.List<?> events = publisher.publishedEvents();
            assertEquals(4, events.size());
            assertInstanceOf(EpisodeScheduled.class, events.get(0));
            assertInstanceOf(AbstractSubmitted.class, events.get(1));
            assertInstanceOf(PresenterAssigned.class, events.get(2));
            assertInstanceOf(SpeakerAssigned.class, events.get(3));
        }

        @Test
        void publishesOneAssignmentEventPerPerson() {
            PersonId p1 = PersonId.random();
            PersonId p2 = PersonId.random();
            PersonId s1 = PersonId.random();
            ComposeEpisodeCommand cmd = new ComposeEpisodeCommand(
                new EpisodeNumber(1),
                new EpisodeTitle("Two hosts"),
                new AirDate(LocalDate.now().plusDays(7)),
                new AbstractText("a".repeat(150)),
                java.util.Set.of(p1, p2),
                java.util.Set.of(s1)
            );
            service.compose(cmd);
            long presenterEvents = publisher.publishedEvents().stream()
                .filter(e -> e instanceof PresenterAssigned).count();
            long speakerEvents = publisher.publishedEvents().stream()
                .filter(e -> e instanceof SpeakerAssigned).count();
            assertEquals(2, presenterEvents);
            assertEquals(1, speakerEvents);
        }

        @Test
        void rejectsDuplicateNumberAndPublishesNothing() {
            seedScheduledEpisode(7);
            EpisodeNumberAlreadyExists ex = assertThrows(EpisodeNumberAlreadyExists.class,
                () -> service.compose(validCommand(7)));
            assertEquals(new EpisodeNumber(7), ex.number());
            assertTrue(publisher.publishedEvents().isEmpty());
            assertEquals(1, repository.size());
        }

        @Test
        void rejectsAirDateInPast() {
            ComposeEpisodeCommand cmd = new ComposeEpisodeCommand(
                new EpisodeNumber(1),
                new EpisodeTitle("Stale"),
                new AirDate(LocalDate.now().minusDays(1)),
                new AbstractText("a".repeat(150)),
                java.util.Set.of(PersonId.random()),
                java.util.Set.of(PersonId.random())
            );
            assertThrows(AirDateInPast.class, () -> service.compose(cmd));
            assertEquals(0, repository.size());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void rejectsEmptyPresenters() {
            assertThrows(IllegalArgumentException.class,
                () -> new ComposeEpisodeCommand(
                    new EpisodeNumber(1),
                    new EpisodeTitle("Bad"),
                    new AirDate(LocalDate.now().plusDays(1)),
                    new AbstractText("a".repeat(150)),
                    java.util.Set.of(),
                    java.util.Set.of(PersonId.random())
                ));
        }

        @Test
        void rejectsEmptySpeakers() {
            assertThrows(IllegalArgumentException.class,
                () -> new ComposeEpisodeCommand(
                    new EpisodeNumber(1),
                    new EpisodeTitle("Bad"),
                    new AirDate(LocalDate.now().plusDays(1)),
                    new AbstractText("a".repeat(150)),
                    java.util.Set.of(PersonId.random()),
                    java.util.Set.of()
                ));
        }
    }
}
