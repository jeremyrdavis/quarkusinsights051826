package io.arrogantprogrammer.quarkusinsights.people.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonSummary;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Read-side adapter implementing the {@link PersonQueries} port.
 *
 * <p>Delegates to the write-side {@link PersonRepository} and projects
 * the full Person aggregate onto the thin {@link PersonSummary} DTO
 * that cross-context consumers (notably the Catalog projector in Plan 5)
 * require. Keeping the implementation here (in the persistence layer
 * rather than the application layer) follows the same pattern as the
 * Programming context: the query adapter is part of the infrastructure,
 * consuming the repository port rather than duplicating its query logic.
 *
 * <p>Part of the People bounded context, infrastructure layer.
 */
@ApplicationScoped
public class PersonQueriesImpl implements PersonQueries {

    private final PersonRepository repository;

    /**
     * Creates the queries adapter.
     *
     * @param repository the Person repository port
     */
    @Inject
    public PersonQueriesImpl(PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PersonSummary> findById(PersonId id) {
        return repository.findById(id)
            .map(p -> new PersonSummary(p.id(), p.name().displayName(), p.email().value()));
    }
}
