package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumberAlreadyExists;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link EpisodeRepositoryImpl} against a real
 * Postgres (Dev Services). Each test runs in its own transaction
 * that is rolled back on completion via {@link TestTransaction}.
 */
@QuarkusTest
class EpisodeRepositoryImplTest {

    @Inject
    EpisodeRepository repository;

    @Test
    @TestTransaction
    void saveAndFindById() {
        Episode episode = Episode.schedule(
            new EpisodeNumber(101),
            new EpisodeTitle("Round trip via DB"),
            new AirDate(LocalDate.now().plusDays(1))
        );
        episode.clearRecordedEvents();

        repository.save(episode);

        Optional<Episode> loaded = repository.findById(episode.id());
        assertTrue(loaded.isPresent());
        assertEquals(episode.id(), loaded.get().id());
        assertEquals(EpisodeStatus.SCHEDULED, loaded.get().status());
    }

    @Test
    @TestTransaction
    void findByIdReturnsEmptyWhenAbsent() {
        Optional<Episode> loaded = repository.findById(
            io.arrogantprogrammer.quarkusinsights.shared.EpisodeId.random());
        assertTrue(loaded.isEmpty());
    }

    @Test
    @TestTransaction
    void findByNumberLocatesByUniqueNumber() {
        Episode episode = Episode.schedule(
            new EpisodeNumber(102),
            new EpisodeTitle("Find by number"),
            new AirDate(LocalDate.now().plusDays(1))
        );
        episode.clearRecordedEvents();
        repository.save(episode);

        Optional<Episode> loaded = repository.findByNumber(new EpisodeNumber(102));
        assertTrue(loaded.isPresent());
        assertEquals(episode.id(), loaded.get().id());
    }

    @Test
    @TestTransaction
    void findByStatusReturnsMatching() {
        Episode a = Episode.schedule(new EpisodeNumber(103), new EpisodeTitle("A"),
            new AirDate(LocalDate.now().plusDays(1)));
        Episode b = Episode.schedule(new EpisodeNumber(104), new EpisodeTitle("B"),
            new AirDate(LocalDate.now().plusDays(2)));
        a.clearRecordedEvents();
        b.clearRecordedEvents();
        repository.save(a);
        repository.save(b);

        List<Episode> scheduled = repository.findByStatus(EpisodeStatus.SCHEDULED);
        assertTrue(scheduled.stream().anyMatch(e -> e.id().equals(a.id())));
        assertTrue(scheduled.stream().anyMatch(e -> e.id().equals(b.id())));
    }

    @Test
    @TestTransaction
    void saveUpdatesExistingEpisodePreservingId() {
        Episode episode = Episode.schedule(
            new EpisodeNumber(105),
            new EpisodeTitle("To be updated"),
            new AirDate(LocalDate.now().plusDays(1))
        );
        episode.clearRecordedEvents();
        repository.save(episode);

        // Mutate and save again
        Episode loaded = repository.findById(episode.id()).orElseThrow();
        loaded.submitAbstract(new AbstractText("a".repeat(150)));
        loaded.assignPresenter(PersonId.random());
        loaded.clearRecordedEvents();
        repository.save(loaded);

        Episode reloaded = repository.findById(episode.id()).orElseThrow();
        assertEquals(episode.id(), reloaded.id());
        assertNotNull(reloaded.theAbstract());
        assertEquals(1, reloaded.presenters().size());
    }

    @Test
    @TestTransaction
    void saveThrowsEpisodeNumberAlreadyExistsOnDuplicateNumber() {
        // First save with number 106 succeeds
        Episode first = Episode.schedule(
            new EpisodeNumber(106),
            new EpisodeTitle("First"),
            new AirDate(LocalDate.now().plusDays(1))
        );
        first.clearRecordedEvents();
        repository.save(first);

        // Second save with same number, different aggregate: must throw
        Episode duplicate = Episode.schedule(
            new EpisodeNumber(106),
            new EpisodeTitle("Duplicate"),
            new AirDate(LocalDate.now().plusDays(2))
        );
        duplicate.clearRecordedEvents();

        EpisodeNumberAlreadyExists ex = assertThrows(EpisodeNumberAlreadyExists.class,
            () -> repository.save(duplicate));
        assertEquals(new EpisodeNumber(106), ex.number());
    }
}
