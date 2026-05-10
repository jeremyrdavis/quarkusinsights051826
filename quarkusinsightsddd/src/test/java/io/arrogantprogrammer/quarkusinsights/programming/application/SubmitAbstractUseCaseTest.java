package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractSubmitted;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition;
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
 * Verifies {@link SubmitAbstractUseCase} behavior using stub
 * implementations of the {@code EpisodeRepository} and
 * {@code DomainEventPublisher} ports — no Quarkus startup needed.
 */
class SubmitAbstractUseCaseTest {

    private InMemoryEpisodeRepository repository;
    private RecordingDomainEventPublisher publisher;
    private SubmitAbstractUseCase useCase;

    private static final AbstractText sampleText =
        new AbstractText("a".repeat(150));

    @BeforeEach
    void setUp() {
        repository = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        useCase = new SubmitAbstractUseCase(repository, publisher);
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
    void handleAttachesAbstractAndPublishesEvent() {
        Episode episode = seedScheduledEpisode();

        useCase.handle(new SubmitAbstractCommand(episode.id(), sampleText));

        Episode persisted = repository.findById(episode.id()).orElseThrow();
        assertEquals(sampleText, persisted.theAbstract().text());
        assertEquals(1, publisher.publishedEvents().size());
        assertInstanceOf(AbstractSubmitted.class, publisher.publishedEvents().get(0));
    }

    @Test
    void handleThrowsEpisodeNotFoundWhenMissing() {
        EpisodeId missing = EpisodeId.random();
        EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
            () -> useCase.handle(new SubmitAbstractCommand(missing, sampleText)));
        assertEquals(missing, ex.episodeId());
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handlePropagatesIllegalEpisodeTransitionWhenLive() {
        // Build an Episode in LIVE status (not SCHEDULED)
        Episode episode = Episode.schedule(
            new EpisodeNumber(2),
            new EpisodeTitle("Live one"),
            new AirDate(LocalDate.now())
        );
        episode.submitAbstract(sampleText);
        episode.assignPresenter(PersonId.random());
        episode.assignSpeaker(PersonId.random());
        episode.goLive();
        episode.clearRecordedEvents();
        repository.save(episode);

        assertThrows(IllegalEpisodeTransition.class,
            () -> useCase.handle(new SubmitAbstractCommand(episode.id(), sampleText)));
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handleClearsRecordedEventsAfterPublishing() {
        Episode episode = seedScheduledEpisode();

        useCase.handle(new SubmitAbstractCommand(episode.id(), sampleText));

        Episode persisted = repository.findById(episode.id()).orElseThrow();
        assertTrue(persisted.recordedEvents().isEmpty());
    }
}
