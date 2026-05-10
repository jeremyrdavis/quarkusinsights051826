package io.arrogantprogrammer.quarkusinsights.people.infrastructure;

import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CDI test stub for {@link PersonRepository}, satisfying the Quarkus
 * CDI container during {@code @QuarkusTest} startup while the real
 * infrastructure implementation (Bundle 2) is not yet available.
 *
 * <p>This is NOT used by the pure-JUnit {@code PersonServiceTest} — that
 * test uses {@code InMemoryPersonRepository} constructed directly.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class StubPersonRepository implements PersonRepository {

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
}
