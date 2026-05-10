package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeCanceled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
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
 * Verifies {@link CancelEpisodeUseCase} behavior using stub
 * implementations of the {@code EpisodeRepository} and
 * {@code DomainEventPublisher} ports — no Quarkus startup needed.
 */
class CancelEpisodeUseCaseTest {

    private InMemoryEpisodeRepository repository;
    private RecordingDomainEventPublisher publisher;
    private CancelEpisodeUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        useCase = new CancelEpisodeUseCase(repository, publisher);
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
    void handleCancelsAndPublishesEventWithReason() {
        Episode episode = seedScheduledEpisode();

        useCase.handle(new CancelEpisodeCommand(episode.id(), "Studio booking conflict"));

        Episode persisted = repository.findById(episode.id()).orElseThrow();
        assertEquals(EpisodeStatus.CANCELED, persisted.status());
        assertEquals(1, publisher.publishedEvents().size());
        EpisodeCanceled event = assertInstanceOf(EpisodeCanceled.class,
            publisher.publishedEvents().get(0));
        assertEquals("Studio booking conflict", event.reason());
    }

    @Test
    void handleThrowsEpisodeNotFoundWhenMissing() {
        EpisodeId missing = EpisodeId.random();

        EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
            () -> useCase.handle(new CancelEpisodeCommand(missing, "any reason")));
        assertEquals(missing, ex.episodeId());
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handlePropagatesIllegalEpisodeTransitionWhenLive() {
        // Episode in LIVE status — cancel requires SCHEDULED
        Episode episode = Episode.schedule(
            new EpisodeNumber(2),
            new EpisodeTitle("Live one"),
            new AirDate(LocalDate.now())
        );
        episode.submitAbstract(new AbstractText("a".repeat(150)));
        episode.assignPresenter(PersonId.random());
        episode.assignSpeaker(PersonId.random());
        episode.goLive();
        episode.clearRecordedEvents();
        repository.save(episode);

        assertThrows(IllegalEpisodeTransition.class,
            () -> useCase.handle(new CancelEpisodeCommand(episode.id(), "too late")));
        assertTrue(publisher.publishedEvents().isEmpty());
    }
}
