package io.arrogantprogrammer.quarkusinsights.people.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonSummary;
import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link PersonRepositoryImpl} against a real
 * Postgres (Dev Services). Each test runs in its own transaction that
 * is rolled back on completion via {@link TestTransaction}.
 */
@QuarkusTest
class PersonRepositoryImplTest {

    @Inject
    PersonRepository repository;

    @Inject
    PersonQueries personQueries;

    private static final PersonName NAME = new PersonName("Holly", "Cummins");
    private static final Bio BIO = new Bio("A".repeat(50));

    private Person newPerson(String emailAddr) {
        Person p = Person.register(NAME, new Email(emailAddr), BIO);
        p.clearRecordedEvents();
        return p;
    }

    @Test
    @TestTransaction
    void saveAndFindById() {
        Person person = newPerson("holly@example.com");
        repository.save(person);

        Optional<Person> loaded = repository.findById(person.id());
        assertTrue(loaded.isPresent());
        assertEquals(person.id(), loaded.get().id());
        assertEquals("Holly", loaded.get().name().first());
        assertEquals("Cummins", loaded.get().name().last());
    }

    @Test
    @TestTransaction
    void findByIdReturnsEmptyWhenAbsent() {
        Optional<Person> loaded = repository.findById(PersonId.random());
        assertTrue(loaded.isEmpty());
    }

    @Test
    @TestTransaction
    void saveUpdatesExistingPersonPreservingId() {
        Person person = newPerson("update@example.com");
        repository.save(person);

        Person loaded = repository.findById(person.id()).orElseThrow();
        PersonName newName = new PersonName("Updated", "Name");
        loaded.rename(newName);
        loaded.clearRecordedEvents();
        repository.save(loaded);

        Person reloaded = repository.findById(person.id()).orElseThrow();
        assertEquals(person.id(), reloaded.id());
        assertEquals(newName, reloaded.name());
    }

    @Test
    @TestTransaction
    void findAllPaginates() {
        Person p1 = newPerson("page1@example.com");
        Person p2 = newPerson("page2@example.com");
        Person p3 = newPerson("page3@example.com");
        repository.save(p1);
        repository.save(p2);
        repository.save(p3);

        // Flush so the sort order is stable
        PersonEntity.flush();

        List<Person> page = repository.findAll(0, 2);
        // Page size is 2, so we get at most 2 results
        assertTrue(page.size() <= 2);
        assertNotNull(page);
    }

    @Test
    @TestTransaction
    void personQueriesReturnsSummary() {
        Person person = newPerson("queries@example.com");
        repository.save(person);

        Optional<PersonSummary> summary = personQueries.findById(person.id());
        assertTrue(summary.isPresent());
        assertEquals(person.id(), summary.get().id());
        assertEquals("Holly Cummins", summary.get().displayName());
        assertEquals("queries@example.com", summary.get().email());
    }
}
