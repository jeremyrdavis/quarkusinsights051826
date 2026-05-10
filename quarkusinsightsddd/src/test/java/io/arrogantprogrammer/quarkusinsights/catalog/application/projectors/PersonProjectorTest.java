package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PresenterEmbeddable;
import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRenamed;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
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
 * Integration tests for {@link PersonProjector} verifying that a
 * {@code PersonRenamed} event cascades into the embedded presenter and speaker
 * collections of affected {@link PublicEpisodeViewEntity} rows.
 */
@QuarkusTest
class PersonProjectorTest {

    @Inject
    Event<DomainEvent> domainEvents;

    @Test
    void personRenamedCascadesIntoPresenterDisplayName() {
        PersonId personId = PersonId.random();

        // Seed a view with this person as a presenter with an old name
        UUID viewId = seedViewWithPresenter(personId.value(), "Old Name");

        // Fire PersonRenamed
        PersonRenamed event = new PersonRenamed(
            personId,
            new PersonName("New", "Name"),
            Instant.now()
        );
        domainEvents.fire(event);

        // Verify the display name was updated
        PublicEpisodeViewEntity found = loadView(viewId);
        assertNotNull(found);
        assertEquals(1, found.presenters.size());
        PresenterEmbeddable presenter = found.presenters.iterator().next();
        assertEquals("New Name", presenter.displayName, "displayName should be updated after PersonRenamed");

        // cleanup
        deleteView(viewId);
    }

    @Test
    void personRenamedDoesNotAffectUnrelatedViews() {
        PersonId renamedPerson = PersonId.random();
        PersonId otherPerson = PersonId.random();

        // Seed a view with a DIFFERENT person as presenter
        UUID viewId = seedViewWithPresenter(otherPerson.value(), "Other Person");

        domainEvents.fire(new PersonRenamed(
            renamedPerson,
            new PersonName("Renamed", "Person"),
            Instant.now()
        ));

        PublicEpisodeViewEntity found = loadView(viewId);
        assertNotNull(found);
        PresenterEmbeddable presenter = found.presenters.iterator().next();
        assertEquals("Other Person", presenter.displayName, "Unrelated person's name should not change");

        deleteView(viewId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Transactional
    UUID seedViewWithPresenter(UUID personId, String displayName) {
        PublicEpisodeViewEntity entity = new PublicEpisodeViewEntity();
        entity.id = UUID.randomUUID();
        entity.number = 800 + (int) (Math.random() * 100);
        entity.title = "Person Projector Test";
        entity.airDate = LocalDate.now().plusDays(5);
        entity.status = EpisodeStatus.SCHEDULED;
        entity.commentCount = 0;
        entity.ratingCount = 0;
        entity.lastUpdatedAt = Instant.now();
        entity.presenters.add(new PresenterEmbeddable(personId, displayName));
        entity.persist();
        return entity.id;
    }

    @Transactional
    PublicEpisodeViewEntity loadView(UUID id) {
        return PublicEpisodeViewEntity.findById(id);
    }

    @Transactional
    void deleteView(UUID id) {
        PublicEpisodeViewEntity entity = PublicEpisodeViewEntity.findById(id);
        if (entity != null) entity.delete();
    }
}
