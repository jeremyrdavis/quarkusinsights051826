package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition;
import io.arrogantprogrammer.quarkusinsights.programming.domain.PresenterAssigned;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AssignPresenterUseCase} behavior using stub
 * implementations of the {@code EpisodeRepository} and
 * {@code DomainEventPublisher} ports — no Quarkus startup needed.
 */
class AssignPresenterUseCaseTest {

    private InMemoryEpisodeRepository repository;
    private RecordingDomainEventPublisher publisher;
    private AssignPresenterUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        useCase = new AssignPresenterUseCase(repository, publisher);
    }

    private Episode seedScheduledEpisode() {
        Episode episode = Episode.schedule(
            new EpisodeNumber(1),
            new EpisodeTitle("Pilot"),
            new AirDate(LocalDate.now().plusDays(7))
        );
        episode.clearRecordedEvents();
        repository.save(episode);
        return episode;
    }

    @Test
    void handleAddsPresenterAndPublishesEvent() {
        Episode episode = seedScheduledEpisode();
        PersonId personId = PersonId.random();

        useCase.handle(new AssignPresenterCommand(episode.id(), personId));

        Episode persisted = repository.findById(episode.id()).orElseThrow();
        assertTrue(persisted.presenters().contains(personId));
        assertEquals(1, publisher.publishedEvents().size());
        PresenterAssigned event = assertInstanceOf(PresenterAssigned.class,
            publisher.publishedEvents().get(0));
        assertEquals(episode.id(), event.episodeId());
        assertEquals(personId, event.personId());
    }

    @Test
    void handleDuplicateAssignmentPublishesNoEvent() {
        Episode episode = seedScheduledEpisode();
        PersonId personId = PersonId.random();

        useCase.handle(new AssignPresenterCommand(episode.id(), personId));
        // Reset to isolate the second call's effect
        publisher.publishedEvents().getClass(); // sanity
        RecordingDomainEventPublisher publisher2 = new RecordingDomainEventPublisher();
        AssignPresenterUseCase useCase2 = new AssignPresenterUseCase(repository, publisher2);

        useCase2.handle(new AssignPresenterCommand(episode.id(), personId));

        assertTrue(publisher2.publishedEvents().isEmpty());
        assertEquals(1, repository.findById(episode.id()).orElseThrow().presenters().size());
    }

    @Test
    void handleThrowsEpisodeNotFoundWhenMissing() {
        EpisodeId missing = EpisodeId.random();

        EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
            () -> useCase.handle(new AssignPresenterCommand(missing, PersonId.random())));
        assertEquals(missing, ex.episodeId());
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handlePropagatesIllegalEpisodeTransitionWhenPublished() {
        // Build an Episode in PUBLISHED status (terminal)
        Episode episode = Episode.schedule(
            new EpisodeNumber(2),
            new EpisodeTitle("Done"),
            new AirDate(LocalDate.now())
        );
        episode.submitAbstract(new io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText("a".repeat(150)));
        episode.assignPresenter(PersonId.random());
        episode.assignSpeaker(PersonId.random());
        episode.goLive();
        episode.publish();
        episode.clearRecordedEvents();
        repository.save(episode);

        assertThrows(IllegalEpisodeTransition.class,
            () -> useCase.handle(new AssignPresenterCommand(episode.id(), PersonId.random())));
        assertTrue(publisher.publishedEvents().isEmpty());
    }
}
