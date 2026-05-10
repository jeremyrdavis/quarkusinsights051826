package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonBioUpdated;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonNotFound;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRegistered;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRenamed;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonSocialsUpdated;
import io.arrogantprogrammer.quarkusinsights.people.domain.SocialLinks;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.arrogantprogrammer.quarkusinsights.shared.application.RecordingDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link PersonService} behavior using stub implementations
 * of the {@code PersonRepository} and {@code DomainEventPublisher}
 * ports — no Quarkus startup needed.
 *
 * <p>Tests are grouped by service method via {@link Nested} classes.
 */
class PersonServiceTest {

    private InMemoryPersonRepository repository;
    private RecordingDomainEventPublisher publisher;
    private PersonService service;

    private static final PersonName VALID_NAME = new PersonName("Jane", "Smith");
    private static final Email VALID_EMAIL = new Email("jane.smith@example.com");
    private static final Bio VALID_BIO = new Bio("A".repeat(50));

    @BeforeEach
    void setUp() {
        repository = new InMemoryPersonRepository();
        publisher = new RecordingDomainEventPublisher();
        service = new PersonService(repository, publisher);
    }

    private Person seedPerson() {
        Person person = Person.register(VALID_NAME, VALID_EMAIL, VALID_BIO);
        person.clearRecordedEvents();
        repository.save(person);
        return person;
    }

    // -----------------------------------------------------------------------
    // Register
    // -----------------------------------------------------------------------

    @Nested
    class Register {

        @Test
        void persistsAndReturnsId() {
            RegisterPersonCommand cmd = new RegisterPersonCommand(
                VALID_NAME, VALID_EMAIL, VALID_BIO);
            PersonId id = service.register(cmd);
            assertNotNull(id);
            assertEquals(1, repository.size());
            Person persisted = repository.findById(id).orElseThrow();
            assertEquals(VALID_NAME, persisted.name());
        }

        @Test
        void publishesPersonRegisteredEvent() {
            service.register(new RegisterPersonCommand(VALID_NAME, VALID_EMAIL, VALID_BIO));
            assertEquals(1, publisher.publishedEvents().size());
            PersonRegistered event = assertInstanceOf(PersonRegistered.class,
                publisher.publishedEvents().get(0));
            assertEquals(VALID_NAME, event.name());
            assertEquals(VALID_EMAIL, event.email());
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            PersonId id = service.register(
                new RegisterPersonCommand(VALID_NAME, VALID_EMAIL, VALID_BIO));
            Person persisted = repository.findById(id).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Rename
    // -----------------------------------------------------------------------

    @Nested
    class Rename {

        @Test
        void throwsPersonNotFoundWhenMissing() {
            PersonId missing = PersonId.random();
            PersonNotFound ex = assertThrows(PersonNotFound.class,
                () -> service.rename(new RenamePersonCommand(
                    missing, new PersonName("John", "Doe"))));
            assertEquals(missing, ex.personId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void persistsRenameAndPublishesEvent() {
            Person person = seedPerson();
            PersonName newName = new PersonName("John", "Doe");
            service.rename(new RenamePersonCommand(person.id(), newName));
            Person persisted = repository.findById(person.id()).orElseThrow();
            assertEquals(newName, persisted.name());
            assertEquals(1, publisher.publishedEvents().size());
            PersonRenamed event = assertInstanceOf(PersonRenamed.class,
                publisher.publishedEvents().get(0));
            assertEquals(newName, event.newName());
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            Person person = seedPerson();
            service.rename(new RenamePersonCommand(person.id(),
                new PersonName("John", "Doe")));
            Person persisted = repository.findById(person.id()).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // UpdateBio
    // -----------------------------------------------------------------------

    @Nested
    class UpdateBio {

        @Test
        void throwsPersonNotFoundWhenMissing() {
            PersonId missing = PersonId.random();
            PersonNotFound ex = assertThrows(PersonNotFound.class,
                () -> service.updateBio(new UpdateBioCommand(
                    missing, new Bio("B".repeat(50)))));
            assertEquals(missing, ex.personId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void persistsBioUpdateAndPublishesEvent() {
            Person person = seedPerson();
            Bio newBio = new Bio("B".repeat(50));
            service.updateBio(new UpdateBioCommand(person.id(), newBio));
            Person persisted = repository.findById(person.id()).orElseThrow();
            assertEquals(newBio, persisted.bio());
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(PersonBioUpdated.class, publisher.publishedEvents().get(0));
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            Person person = seedPerson();
            service.updateBio(new UpdateBioCommand(person.id(), new Bio("B".repeat(50))));
            Person persisted = repository.findById(person.id()).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // UpdateSocials
    // -----------------------------------------------------------------------

    @Nested
    class UpdateSocials {

        @Test
        void throwsPersonNotFoundWhenMissing() {
            PersonId missing = PersonId.random();
            PersonNotFound ex = assertThrows(PersonNotFound.class,
                () -> service.updateSocials(new UpdateSocialsCommand(
                    missing, SocialLinks.none())));
            assertEquals(missing, ex.personId());
            assertTrue(publisher.publishedEvents().isEmpty());
        }

        @Test
        void persistsSocialsUpdateAndPublishesEvent() {
            Person person = seedPerson();
            SocialLinks newSocials = new SocialLinks(
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.of(java.net.URI.create("https://jsmith.dev"))
            );
            service.updateSocials(new UpdateSocialsCommand(person.id(), newSocials));
            Person persisted = repository.findById(person.id()).orElseThrow();
            assertEquals(newSocials, persisted.socials());
            assertEquals(1, publisher.publishedEvents().size());
            assertInstanceOf(PersonSocialsUpdated.class, publisher.publishedEvents().get(0));
        }

        @Test
        void clearsRecordedEventsAfterPublishing() {
            Person person = seedPerson();
            service.updateSocials(new UpdateSocialsCommand(person.id(), SocialLinks.none()));
            Person persisted = repository.findById(person.id()).orElseThrow();
            assertTrue(persisted.recordedEvents().isEmpty());
        }
    }
}
