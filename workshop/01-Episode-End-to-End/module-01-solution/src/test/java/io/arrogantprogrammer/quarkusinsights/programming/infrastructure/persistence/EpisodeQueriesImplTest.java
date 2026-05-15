package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeService;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeSummary;
import io.arrogantprogrammer.quarkusinsights.programming.application.ScheduleEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.SubmitAbstractCommand;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link EpisodeQueriesImpl} covering the
 * cross-context read port round-trip against a real PostgreSQL database
 * (Quarkus Dev Services). Each test is rolled back via {@link TestTransaction}.
 */
@QuarkusTest
class EpisodeQueriesImplTest {

    @Inject
    EpisodeQueries episodeQueries;

    @Inject
    EpisodeService episodeService;

    @Test
    @TestTransaction
    void findByIdReturnsEmptyForUnknownId() {
        Optional<EpisodeSummary> result = episodeQueries.findById(EpisodeId.random());
        assertTrue(result.isEmpty(), "Expected empty for a non-existent EpisodeId");
    }

    @Test
    @TestTransaction
    void findByIdReturnsSummaryWithCorrectTitleAndNoAbstract() {
        EpisodeId episodeId = episodeService.schedule(new ScheduleEpisodeCommand(
            new EpisodeNumber(9200),
            new EpisodeTitle("Round Trip Title"),
            new AirDate(LocalDate.now().plusDays(1))
        ));

        Optional<EpisodeSummary> result = episodeQueries.findById(episodeId);

        assertTrue(result.isPresent(), "Summary should be found for a just-scheduled episode");
        EpisodeSummary summary = result.get();
        assertEquals("Round Trip Title", summary.title().value());
        assertEquals(9200, summary.number().value());
        assertEquals(EpisodeStatus.SCHEDULED, summary.status());
        assertFalse(summary.abstractText().isPresent(),
            "No abstract should be present before submitAbstract is called");
    }

    @Test
    @TestTransaction
    void findByIdReturnsRealAbstractTextAfterSubmit() {
        EpisodeId episodeId = episodeService.schedule(new ScheduleEpisodeCommand(
            new EpisodeNumber(9201),
            new EpisodeTitle("Abstract Round Trip"),
            new AirDate(LocalDate.now().plusDays(1))
        ));

        String abstractBody = "B".repeat(200); // valid 200-char abstract
        episodeService.submitAbstract(new SubmitAbstractCommand(
            episodeId,
            new AbstractText(abstractBody)
        ));

        Optional<EpisodeSummary> result = episodeQueries.findById(episodeId);

        assertTrue(result.isPresent());
        assertTrue(result.get().abstractText().isPresent(),
            "Abstract should be present after submitAbstract");
        assertEquals(abstractBody, result.get().abstractText().get().value());
    }
}
