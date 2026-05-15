package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link EpisodeMapper} round-trips an Episode aggregate
 * through {@link EpisodeEntity} preserving all state. Pure JUnit; no
 * Quarkus startup since the mapper has no CDI dependencies of its
 * own.
 */
class EpisodeMapperTest {

    private EpisodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EpisodeMapper();
    }

    @Test
    void roundTripPreservesScheduledEpisodeWithoutAbstract() {
        Episode original = Episode.schedule(
            new EpisodeNumber(1),
            new EpisodeTitle("Pilot"),
            new AirDate(LocalDate.now().plusDays(7))
        );
        original.clearRecordedEvents();

        EpisodeEntity entity = mapper.toEntity(original);
        Episode roundTripped = mapper.toDomain(entity);

        assertEquals(original.id(), roundTripped.id());
        assertEquals(original.number(), roundTripped.number());
        assertEquals(original.title(), roundTripped.title());
        assertEquals(original.airDate(), roundTripped.airDate());
        assertNull(roundTripped.theAbstract());
        assertTrue(roundTripped.presenters().isEmpty());
        assertTrue(roundTripped.speakers().isEmpty());
        assertEquals(EpisodeStatus.SCHEDULED, roundTripped.status());
    }

    @Test
    void roundTripPreservesEpisodeWithAbstract() {
        Episode original = Episode.schedule(
            new EpisodeNumber(2),
            new EpisodeTitle("Has abstract"),
            new AirDate(LocalDate.now().plusDays(1))
        );
        AbstractText text = new AbstractText("a".repeat(150));
        original.submitAbstract(text);
        original.clearRecordedEvents();

        Episode roundTripped = mapper.toDomain(mapper.toEntity(original));

        assertNotNull(roundTripped.theAbstract());
        assertEquals(text, roundTripped.theAbstract().text());
        assertEquals(original.theAbstract().id(), roundTripped.theAbstract().id());
    }

    @Test
    void roundTripPreservesPresentersAndSpeakers() {
        Episode original = Episode.schedule(
            new EpisodeNumber(3),
            new EpisodeTitle("With cast"),
            new AirDate(LocalDate.now().plusDays(1))
        );
        PersonId presenter1 = PersonId.random();
        PersonId presenter2 = PersonId.random();
        PersonId speaker = PersonId.random();
        original.assignPresenter(presenter1);
        original.assignPresenter(presenter2);
        original.assignSpeaker(speaker);
        original.clearRecordedEvents();

        Episode roundTripped = mapper.toDomain(mapper.toEntity(original));

        assertEquals(2, roundTripped.presenters().size());
        assertTrue(roundTripped.presenters().contains(presenter1));
        assertTrue(roundTripped.presenters().contains(presenter2));
        assertEquals(1, roundTripped.speakers().size());
        assertTrue(roundTripped.speakers().contains(speaker));
    }

    @Test
    void applyToPreservesIdentityAndUpdatesMutableState() {
        Episode original = Episode.schedule(
            new EpisodeNumber(4),
            new EpisodeTitle("Update target"),
            new AirDate(LocalDate.now())
        );
        original.clearRecordedEvents();

        EpisodeEntity entity = mapper.toEntity(original);
        EpisodeId originalEntityId = new EpisodeId(entity.id);

        // Mutate the aggregate
        original.submitAbstract(new AbstractText("a".repeat(200)));
        original.assignPresenter(PersonId.random());

        mapper.applyTo(original, entity);

        // Identity preserved, new state reflected
        assertEquals(originalEntityId.value(), entity.id);
        assertNotNull(entity.theAbstract);
        assertEquals(1, entity.presenters.size());
    }
}
