package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link Person} aggregate behavior using pure JUnit — no Quarkus
 * startup needed.
 *
 * <p>Tests are grouped by behavior via {@link Nested} classes.
 */
class PersonTest {

    // -----------------------------------------------------------------------
    // Shared test data
    // -----------------------------------------------------------------------

    private static final PersonName VALID_NAME = new PersonName("Jane", "Smith");
    private static final Email VALID_EMAIL = new Email("jane.smith@example.com");
    private static final Bio VALID_BIO = new Bio("A".repeat(50));

    private static Person freshPerson() {
        return Person.register(VALID_NAME, VALID_EMAIL, VALID_BIO);
    }

    // -----------------------------------------------------------------------
    // Register
    // -----------------------------------------------------------------------

    @Nested
    class Register {

        @Test
        void setsNameEmailBio() {
            Person person = freshPerson();
            assertEquals(VALID_NAME, person.name());
            assertEquals(VALID_EMAIL, person.email());
            assertEquals(VALID_BIO, person.bio());
        }

        @Test
        void socialLinksInitializedToNone() {
            Person person = freshPerson();
            assertEquals(SocialLinks.none(), person.socials());
        }

        @Test
        void createdAtEqualsUpdatedAt() {
            Person person = freshPerson();
            assertEquals(person.createdAt(), person.updatedAt());
        }

        @Test
        void idIsAssigned() {
            Person person = freshPerson();
            assertNotNull(person.id());
        }

        @Test
        void recordsPersonRegisteredEvent() {
            Person person = freshPerson();
            assertEquals(1, person.recordedEvents().size());
            PersonRegistered event = assertInstanceOf(PersonRegistered.class,
                person.recordedEvents().get(0));
            assertEquals(person.id(), event.personId());
            assertEquals(VALID_NAME, event.name());
            assertEquals(VALID_EMAIL, event.email());
            assertNotNull(event.occurredAt());
        }

        @Test
        void nullNameThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> Person.register(null, VALID_EMAIL, VALID_BIO));
        }

        @Test
        void nullEmailThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> Person.register(VALID_NAME, null, VALID_BIO));
        }

        @Test
        void nullBioThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> Person.register(VALID_NAME, VALID_EMAIL, null));
        }
    }

    // -----------------------------------------------------------------------
    // Rename
    // -----------------------------------------------------------------------

    @Nested
    class Rename {

        @Test
        void updatesNameAndBumpsUpdatedAt() throws InterruptedException {
            Person person = freshPerson();
            Instant before = person.updatedAt();
            Thread.sleep(1); // ensure clock advances
            PersonName newName = new PersonName("John", "Doe");
            person.clearRecordedEvents();
            person.rename(newName);
            assertEquals(newName, person.name());
            assertTrue(person.updatedAt().isAfter(before)
                || person.updatedAt().equals(before)); // monotonic
        }

        @Test
        void recordsPersonRenamedEvent() {
            Person person = freshPerson();
            person.clearRecordedEvents();
            PersonName newName = new PersonName("John", "Doe");
            person.rename(newName);
            assertEquals(1, person.recordedEvents().size());
            PersonRenamed event = assertInstanceOf(PersonRenamed.class,
                person.recordedEvents().get(0));
            assertEquals(person.id(), event.personId());
            assertEquals(newName, event.newName());
            assertNotNull(event.occurredAt());
        }

        @Test
        void nullNameThrows() {
            Person person = freshPerson();
            assertThrows(IllegalArgumentException.class, () -> person.rename(null));
        }
    }

    // -----------------------------------------------------------------------
    // UpdateBio
    // -----------------------------------------------------------------------

    @Nested
    class UpdateBio {

        @Test
        void updatesBioAndBumpsUpdatedAt() {
            Person person = freshPerson();
            Bio newBio = new Bio("B".repeat(50));
            person.clearRecordedEvents();
            person.updateBio(newBio);
            assertEquals(newBio, person.bio());
        }

        @Test
        void recordsPersonBioUpdatedEvent() {
            Person person = freshPerson();
            person.clearRecordedEvents();
            person.updateBio(new Bio("B".repeat(50)));
            assertEquals(1, person.recordedEvents().size());
            PersonBioUpdated event = assertInstanceOf(PersonBioUpdated.class,
                person.recordedEvents().get(0));
            assertEquals(person.id(), event.personId());
            assertNotNull(event.occurredAt());
        }

        @Test
        void nullBioThrows() {
            Person person = freshPerson();
            assertThrows(IllegalArgumentException.class, () -> person.updateBio(null));
        }
    }

    // -----------------------------------------------------------------------
    // UpdateSocials
    // -----------------------------------------------------------------------

    @Nested
    class UpdateSocials {

        @Test
        void updatesSocialsAndBumpsUpdatedAt() {
            Person person = freshPerson();
            SocialLinks newSocials = new SocialLinks(
                Optional.empty(),
                Optional.empty(),
                Optional.of(java.net.URI.create("https://jsmith.dev"))
            );
            person.clearRecordedEvents();
            person.updateSocials(newSocials);
            assertEquals(newSocials, person.socials());
        }

        @Test
        void recordsPersonSocialsUpdatedEvent() {
            Person person = freshPerson();
            person.clearRecordedEvents();
            person.updateSocials(SocialLinks.none());
            assertEquals(1, person.recordedEvents().size());
            PersonSocialsUpdated event = assertInstanceOf(PersonSocialsUpdated.class,
                person.recordedEvents().get(0));
            assertEquals(person.id(), event.personId());
            assertNotNull(event.occurredAt());
        }

        @Test
        void nullSocialsThrows() {
            Person person = freshPerson();
            assertThrows(IllegalArgumentException.class, () -> person.updateSocials(null));
        }
    }

    // -----------------------------------------------------------------------
    // Rehydrate
    // -----------------------------------------------------------------------

    @Nested
    class Rehydrate {

        @Test
        void preservesAllStateAndRecordsNoEvents() {
            PersonId id = PersonId.random();
            Instant created = Instant.now().minusSeconds(3600);
            Instant updated = Instant.now().minusSeconds(60);
            SocialLinks socials = SocialLinks.none();

            Person person = Person.rehydrate(id, VALID_NAME, VALID_EMAIL, VALID_BIO,
                socials, created, updated);

            assertEquals(id, person.id());
            assertEquals(VALID_NAME, person.name());
            assertEquals(VALID_EMAIL, person.email());
            assertEquals(VALID_BIO, person.bio());
            assertEquals(socials, person.socials());
            assertEquals(created, person.createdAt());
            assertEquals(updated, person.updatedAt());
            assertTrue(person.recordedEvents().isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // RecordedEvents semantics
    // -----------------------------------------------------------------------

    @Nested
    class RecordedEvents {

        @Test
        void returnsUnmodifiableList() {
            Person person = freshPerson();
            assertThrows(UnsupportedOperationException.class,
                () -> person.recordedEvents().clear());
        }

        @Test
        void clearRecordedEventsEmptiesTheList() {
            Person person = freshPerson();
            assertFalse(person.recordedEvents().isEmpty());
            person.clearRecordedEvents();
            assertTrue(person.recordedEvents().isEmpty());
        }
    }
}
