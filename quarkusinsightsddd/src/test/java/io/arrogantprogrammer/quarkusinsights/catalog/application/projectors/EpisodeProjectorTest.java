package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonService;
import io.arrogantprogrammer.quarkusinsights.people.application.RegisterPersonCommand;
import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.programming.application.AssignPresenterCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeService;
import io.arrogantprogrammer.quarkusinsights.programming.application.ScheduleEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.SubmitAbstractCommand;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeCanceled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodePublished;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence.EpisodeEntity;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for {@link EpisodeProjector} verifying that domain operations
 * produce the correct denormalized view in {@link PublicEpisodeViewEntity}.
 *
 * <p>Tests seed Episodes via {@link EpisodeService#schedule} (and related commands)
 * so that the Episode aggregate is persisted before the projector's observer fires.
 * This mirrors production behavior where {@code EpisodeScheduled} is only published
 * after the Episode is saved in the same transaction.
 *
 * <p>Status-only transitions ({@link EpisodePublished}, {@link EpisodeCanceled}) are
 * still fired via CDI {@link Event} because the projector only reads the existing
 * view row for those events — no cross-context Episode lookup is required.
 */
@QuarkusTest
class EpisodeProjectorTest {

    @Inject
    Event<DomainEvent> domainEvents;

    @Inject
    EpisodeService episodeService;

    @Inject
    PersonService personService;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void episodeScheduledCreatesViewRowWithRealTitle() {
        EpisodeId episodeId = scheduleEpisode(800, "Pilot Episode", LocalDate.now().plusDays(1));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found, "Entity should have been created by EpisodeProjector");
        assertEquals(800, found.number);
        assertEquals("Pilot Episode", found.title,
            "Title should be the real title, not a placeholder like 'Episode N'");
        assertEquals(EpisodeStatus.SCHEDULED, found.status);
        assertEquals(0, found.commentCount);
        assertEquals(0, found.ratingCount);

        // cleanup
        deleteAll(episodeId);
    }

    @Test
    void abstractSubmittedStoresRealAbstractText() {
        EpisodeId episodeId = scheduleEpisode(801, "Deep Dive Into Quarkus", LocalDate.now().plusDays(1));

        String abstractBody = "A".repeat(150); // valid 150-char abstract
        episodeService.submitAbstract(new SubmitAbstractCommand(
            episodeId,
            new AbstractText(abstractBody)
        ));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertEquals(abstractBody, found.abstractText,
            "abstractText should be the real abstract body, not a sentinel placeholder");

        deleteAll(episodeId);
    }

    @Test
    void presenterAssignedAddsPresenterWithDisplayName() {
        // Register a real Person so PersonQueries can resolve their name
        PersonId personId = registerPerson("projtest-presenter@example.com", "Test", "Presenter");

        EpisodeId episodeId = scheduleEpisode(802, "Presenter Test Episode", LocalDate.now().plusDays(1));
        episodeService.assignPresenter(new AssignPresenterCommand(episodeId, personId));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertEquals(1, found.presenters.size(), "Expected one presenter after assignPresenter");
        var presenter = found.presenters.iterator().next();
        assertEquals(personId.value(), presenter.personId);
        assertEquals("Test Presenter", presenter.displayName);

        deleteAll(episodeId);
    }

    @Test
    void episodePublishedUpdatesStatus() {
        EpisodeId episodeId = scheduleEpisode(803, "Published Episode", LocalDate.now().plusDays(1));

        domainEvents.fire(new EpisodePublished(episodeId, new EpisodeNumber(803), Instant.now()));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertEquals(EpisodeStatus.PUBLISHED, found.status, "Status should be PUBLISHED");

        deleteAll(episodeId);
    }

    @Test
    void episodeCanceledUpdatesStatus() {
        EpisodeId episodeId = scheduleEpisode(804, "Canceled Episode", LocalDate.now().plusDays(1));

        domainEvents.fire(new EpisodeCanceled(episodeId, "Schedule conflict", Instant.now()));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertEquals(EpisodeStatus.CANCELED, found.status, "Status should be CANCELED");

        deleteAll(episodeId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Schedules a new Episode via {@link EpisodeService#schedule}. The service
     * persists the Episode and fires {@code EpisodeScheduled}, which causes the
     * projector to insert a {@link PublicEpisodeViewEntity} row with the real title.
     */
    EpisodeId scheduleEpisode(int number, String title, LocalDate airDate) {
        return episodeService.schedule(new ScheduleEpisodeCommand(
            new EpisodeNumber(number),
            new EpisodeTitle(title),
            new AirDate(airDate)
        ));
    }

    @Transactional
    PublicEpisodeViewEntity findEntity(EpisodeId episodeId) {
        return PublicEpisodeViewEntity.findById(episodeId.value());
    }

    /**
     * Deletes both the {@link PublicEpisodeViewEntity} and the underlying
     * {@link EpisodeEntity} so each test starts with a clean slate.
     */
    @Transactional
    void deleteAll(EpisodeId episodeId) {
        PublicEpisodeViewEntity view = PublicEpisodeViewEntity.findById(episodeId.value());
        if (view != null) view.delete();
        EpisodeEntity episode = EpisodeEntity.findById(episodeId.value());
        if (episode != null) episode.delete();
    }

    PersonId registerPerson(String email, String first, String last) {
        RegisterPersonCommand cmd = new RegisterPersonCommand(
            new PersonName(first, last),
            new Email(email),
            new Bio("A".repeat(50))
        );
        return personService.register(cmd);
    }
}
