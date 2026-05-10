package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonService;
import io.arrogantprogrammer.quarkusinsights.people.application.RegisterPersonCommand;
import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractId;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractSubmitted;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeCanceled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodePublished;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeScheduled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.PresenterAssigned;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for {@link EpisodeProjector} verifying that CDI domain
 * events fired via {@link Event} are observed and result in the correct
 * mutations on {@link PublicEpisodeViewEntity}.
 *
 * <p>Each test is wrapped in its own {@code @Transactional} helper method so
 * that the fired event's observer (which is itself {@code @Transactional})
 * participates in or joins the test's transaction, and the entity is visible
 * immediately after the event fires.
 */
@QuarkusTest
class EpisodeProjectorTest {

    @Inject
    Event<DomainEvent> domainEvents;

    @Inject
    PersonService personService;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void episodeScheduledCreatesViewRow() {
        EpisodeId episodeId = EpisodeId.random();
        EpisodeScheduled event = new EpisodeScheduled(
            episodeId,
            new EpisodeNumber(700),
            new AirDate(LocalDate.now().plusDays(1)),
            Instant.now()
        );

        domainEvents.fire(event);

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found, "Entity should have been created by EpisodeProjector");
        assertEquals(700, found.number);
        assertEquals(EpisodeStatus.SCHEDULED, found.status);
        assertEquals(0, found.commentCount);
        assertEquals(0, found.ratingCount);

        // cleanup
        deleteEntity(episodeId);
    }

    @Test
    void abstractSubmittedUpdatesEntity() {
        EpisodeId episodeId = EpisodeId.random();
        // First schedule so the entity exists
        domainEvents.fire(new EpisodeScheduled(
            episodeId,
            new EpisodeNumber(701),
            new AirDate(LocalDate.now().plusDays(1)),
            Instant.now()
        ));

        AbstractSubmitted event = new AbstractSubmitted(
            episodeId,
            AbstractId.random(),
            Instant.now()
        );
        domainEvents.fire(event);

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertNotNull(found.abstractText, "abstractText should be set after AbstractSubmitted");

        deleteEntity(episodeId);
    }

    @Test
    void presenterAssignedAddsPresenterWithDisplayName() {
        // Register a real Person so PersonQueries can resolve their name
        PersonId personId = registerPerson("projtest-presenter@example.com", "Test", "Presenter");

        EpisodeId episodeId = EpisodeId.random();
        domainEvents.fire(new EpisodeScheduled(
            episodeId,
            new EpisodeNumber(702),
            new AirDate(LocalDate.now().plusDays(1)),
            Instant.now()
        ));

        domainEvents.fire(new PresenterAssigned(episodeId, personId, Instant.now()));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertEquals(1, found.presenters.size(), "Expected one presenter after PresenterAssigned");
        var presenter = found.presenters.iterator().next();
        assertEquals(personId.value(), presenter.personId);
        assertEquals("Test Presenter", presenter.displayName);

        deleteEntity(episodeId);
    }

    @Test
    void episodePublishedUpdatesStatus() {
        EpisodeId episodeId = EpisodeId.random();
        domainEvents.fire(new EpisodeScheduled(
            episodeId,
            new EpisodeNumber(703),
            new AirDate(LocalDate.now().plusDays(1)),
            Instant.now()
        ));

        domainEvents.fire(new EpisodePublished(episodeId, new EpisodeNumber(703), Instant.now()));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertEquals(EpisodeStatus.PUBLISHED, found.status, "Status should be PUBLISHED");

        deleteEntity(episodeId);
    }

    @Test
    void episodeCanceledUpdatesStatus() {
        EpisodeId episodeId = EpisodeId.random();
        domainEvents.fire(new EpisodeScheduled(
            episodeId,
            new EpisodeNumber(704),
            new AirDate(LocalDate.now().plusDays(1)),
            Instant.now()
        ));

        domainEvents.fire(new EpisodeCanceled(episodeId, "Schedule conflict", Instant.now()));

        PublicEpisodeViewEntity found = findEntity(episodeId);
        assertNotNull(found);
        assertEquals(EpisodeStatus.CANCELED, found.status, "Status should be CANCELED");

        deleteEntity(episodeId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Transactional
    PublicEpisodeViewEntity findEntity(EpisodeId episodeId) {
        return PublicEpisodeViewEntity.findById(episodeId.value());
    }

    @Transactional
    void deleteEntity(EpisodeId episodeId) {
        PublicEpisodeViewEntity entity = PublicEpisodeViewEntity.findById(episodeId.value());
        if (entity != null) entity.delete();
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
