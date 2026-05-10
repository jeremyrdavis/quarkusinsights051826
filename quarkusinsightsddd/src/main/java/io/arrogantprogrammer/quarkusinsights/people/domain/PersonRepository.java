package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Person} aggregate root.
 *
 * <p>Defined in the domain layer so application services depend on the
 * port (not on Panache or any other adapter). Implementations live in
 * {@code people/infrastructure/persistence/} and are injected via CDI;
 * the domain layer must not import any concrete implementation.
 *
 * <p><strong>Save semantics.</strong> {@link #save(Person)} is
 * idempotent in identity: calling save twice with the same Person
 * persists the latest state of that aggregate (the implementation
 * resolves whether to insert or update by id). Save MUST NOT publish
 * domain events; that responsibility belongs to the application service
 * after the save commits.
 *
 * <p><strong>Load semantics.</strong> {@link #findById} returns
 * {@link Optional#empty()} when the aggregate is not found rather than
 * throwing — the absence of a Person is a valid query result, not an
 * exception.
 *
 * <p>Part of the People bounded context, domain layer.
 */
public interface PersonRepository {

    /**
     * Loads a Person aggregate by id.
     *
     * @param id the aggregate identifier; must not be null
     * @return the Person, or {@link Optional#empty()} if no Person
     *     with that id exists
     */
    Optional<Person> findById(PersonId id);

    /**
     * Persists a Person aggregate (insert or update by identity).
     *
     * <p>Implementations MUST NOT publish domain events. The
     * application service is responsible for reading
     * {@link Person#recordedEvents()} after a successful save and
     * dispatching them, then calling {@link Person#clearRecordedEvents()}.
     *
     * @param person the aggregate to persist; must not be null
     */
    void save(Person person);

    /**
     * Returns a page of Person aggregates, ordered by registration time
     * (oldest first).
     *
     * @param page zero-based page index; must be &ge; 0
     * @param size maximum number of results per page; must be &ge; 1
     * @return a list of Persons for the requested page (possibly empty)
     */
    List<Person> findAll(int page, int size);
}
