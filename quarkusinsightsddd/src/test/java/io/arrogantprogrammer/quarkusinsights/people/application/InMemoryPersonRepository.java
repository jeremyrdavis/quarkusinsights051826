package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test-only implementation of {@link PersonRepository} backed by a
 * {@link HashMap}. Use case unit tests construct one of these,
 * pre-populate it with seed Persons if needed, then pass it to the
 * service under test.
 */
public class InMemoryPersonRepository implements PersonRepository {

    private final Map<PersonId, Person> store = new HashMap<>();

    @Override
    public Optional<Person> findById(PersonId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Person person) {
        store.put(person.id(), person);
    }

    @Override
    public List<Person> findAll(int page, int size) {
        return store.values().stream()
            .skip((long) page * size)
            .limit(size)
            .toList();
    }

    /**
     * Test helper: returns the current count of stored Persons.
     *
     * @return the count of stored Persons
     */
    public int size() {
        return store.size();
    }
}
