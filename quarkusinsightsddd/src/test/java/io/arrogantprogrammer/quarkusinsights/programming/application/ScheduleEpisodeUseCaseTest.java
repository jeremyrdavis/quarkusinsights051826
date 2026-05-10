package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumberAlreadyExists;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeScheduled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ScheduleEpisodeUseCase} behavior using stub
 * implementations of the {@code EpisodeRepository} and
 * {@code DomainEventPublisher} ports — no Quarkus startup needed.
 */
class ScheduleEpisodeUseCaseTest {

    private InMemoryEpisodeRepository repository;
    private RecordingDomainEventPublisher publisher;
    private ScheduleEpisodeUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        useCase = new ScheduleEpisodeUseCase(repository, publisher);
    }

    @Test
    void handlePersistsAndReturnsId() {
        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            new EpisodeNumber(1),
            new EpisodeTitle("Pilot"),
            new AirDate(LocalDate.now().plusDays(7))
        );

        EpisodeId id = useCase.handle(cmd);

        assertNotNull(id);
        assertEquals(1, repository.size());
        Episode persisted = repository.findById(id).orElseThrow();
        assertEquals(EpisodeStatus.SCHEDULED, persisted.status());
    }

    @Test
    void handlePublishesEpisodeScheduledEvent() {
        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            new EpisodeNumber(2),
            new EpisodeTitle("Episode Two"),
            new AirDate(LocalDate.now().plusDays(14))
        );

        useCase.handle(cmd);

        assertEquals(1, publisher.publishedEvents().size());
        assertInstanceOf(EpisodeScheduled.class, publisher.publishedEvents().get(0));
    }

    @Test
    void handleThrowsWhenNumberAlreadyExists() {
        EpisodeNumber duplicate = new EpisodeNumber(7);
        // Pre-populate with an Episode at number 7
        Episode existing = Episode.schedule(duplicate, new EpisodeTitle("Already there"),
            new AirDate(LocalDate.now().plusDays(1)));
        existing.clearRecordedEvents();
        repository.save(existing);

        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            duplicate,
            new EpisodeTitle("Conflict"),
            new AirDate(LocalDate.now().plusDays(2))
        );

        EpisodeNumberAlreadyExists ex = assertThrows(EpisodeNumberAlreadyExists.class,
            () -> useCase.handle(cmd));
        assertEquals(duplicate, ex.number());
        // Repository unchanged; event not published
        assertEquals(1, repository.size());
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handlePropagatesAirDateInPast() {
        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            new EpisodeNumber(3),
            new EpisodeTitle("Past"),
            new AirDate(LocalDate.now().minusDays(1))
        );

        assertThrows(AirDateInPast.class, () -> useCase.handle(cmd));
        // Save was never reached
        assertEquals(0, repository.size());
        assertTrue(publisher.publishedEvents().isEmpty());
    }

    @Test
    void handleClearsRecordedEventsAfterPublishing() {
        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            new EpisodeNumber(4),
            new EpisodeTitle("Clear test"),
            new AirDate(LocalDate.now().plusDays(3))
        );

        EpisodeId id = useCase.handle(cmd);

        Episode persisted = repository.findById(id).orElseThrow();
        assertTrue(persisted.recordedEvents().isEmpty());
    }
}
