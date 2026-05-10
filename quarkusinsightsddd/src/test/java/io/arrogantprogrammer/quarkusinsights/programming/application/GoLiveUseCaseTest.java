package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeWentLive;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link GoLiveUseCase} behavior using stub implementations
 * of the {@code EpisodeRepository} and {@code DomainEventPublisher}
 * ports — no Quarkus startup needed.
 */
class GoLiveUseCaseTest {

    private InMemoryEpisodeRepository repository;
    private RecordingDomainEventPublisher publisher;
    private GoLiveUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        useCase = new GoLiveUseCase(repository, publisher);
    }

    @Test
    void handleTransitionsToLiveAndPublishesEvent() {
        Episode episode = Episode.schedule(
            new EpisodeNumber(1),
            new EpisodeTitle("Pilot"),
            new AirDate(LocalDate.now())
        );
        episode.clearRecordedEvents();
        repository.save(episode);

        useCase.handle(new GoLiveCommand(episode.id()));

        Episode persisted = repository.findById(episode.id()).orElseThrow();
        assertEquals(EpisodeStatus.LIVE, persisted.status());
        assertEquals(1, publisher.publishedEvents().size());
        assertInstanceOf(EpisodeWentLive.class, publisher.publishedEvents().get(0));
    }

    @Test
    void handleThrowsEpisodeNotFoundWhenMissing() {
        EpisodeId missing = EpisodeId.random();

        EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
            () -> useCase.handle(new GoLiveCommand(missing)));
        assertEquals(missing, ex.episodeId());
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handlePropagatesAirDateInPastWhenNotYet() {
        // AirDate is in the future — goLive should refuse
        Episode episode = Episode.schedule(
            new EpisodeNumber(2),
            new EpisodeTitle("Future"),
            new AirDate(LocalDate.now().plusDays(7))
        );
        episode.clearRecordedEvents();
        repository.save(episode);

        assertThrows(AirDateInPast.class,
            () -> useCase.handle(new GoLiveCommand(episode.id())));
        assertTrue(publisher.publishedEvents().isEmpty());
    }
}
