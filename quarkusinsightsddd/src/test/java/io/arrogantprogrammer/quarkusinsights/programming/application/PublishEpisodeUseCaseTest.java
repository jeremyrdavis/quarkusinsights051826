package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodePublished;
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
 * Verifies {@link PublishEpisodeUseCase} behavior using stub
 * implementations of the {@code EpisodeRepository} and
 * {@code DomainEventPublisher} ports — no Quarkus startup needed.
 *
 * <p>The aggregate's full publish-precondition matrix is exhaustively
 * tested in {@code EpisodeTest}; this class verifies that the use
 * case correctly propagates the aggregate exceptions and follows the
 * load-invoke-save-publish pattern.
 */
class PublishEpisodeUseCaseTest {

    private InMemoryEpisodeRepository repository;
    private RecordingDomainEventPublisher publisher;
    private PublishEpisodeUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        useCase = new PublishEpisodeUseCase(repository, publisher);
    }

    private Episode seedLiveEpisodeReadyToPublish() {
        Episode episode = Episode.schedule(
            new EpisodeNumber(1),
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

    @Test
    void handleTransitionsToPublishedAndPublishesEvent() {
        Episode episode = seedLiveEpisodeReadyToPublish();

        useCase.handle(new PublishEpisodeCommand(episode.id()));

        Episode persisted = repository.findById(episode.id()).orElseThrow();
        assertEquals(EpisodeStatus.PUBLISHED, persisted.status());
        assertEquals(1, publisher.publishedEvents().size());
        assertInstanceOf(EpisodePublished.class, publisher.publishedEvents().get(0));
    }

    @Test
    void handleThrowsEpisodeNotFoundWhenMissing() {
        EpisodeId missing = EpisodeId.random();

        EpisodeNotFound ex = assertThrows(EpisodeNotFound.class,
            () -> useCase.handle(new PublishEpisodeCommand(missing)));
        assertEquals(missing, ex.episodeId());
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handlePropagatesIllegalEpisodeTransitionWhenScheduled() {
        // Episode in SCHEDULED status — publish requires LIVE
        Episode episode = Episode.schedule(
            new EpisodeNumber(2),
            new EpisodeTitle("Not yet live"),
            new AirDate(LocalDate.now().plusDays(7))
        );
        episode.clearRecordedEvents();
        repository.save(episode);

        assertThrows(IllegalEpisodeTransition.class,
            () -> useCase.handle(new PublishEpisodeCommand(episode.id())));
        assertTrue(publisher.publishedEvents().isEmpty());
    }
}
