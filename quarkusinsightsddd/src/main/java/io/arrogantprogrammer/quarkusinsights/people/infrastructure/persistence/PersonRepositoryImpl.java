package io.arrogantprogrammer.quarkusinsights.people.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of the {@link PersonRepository} port.
 *
 * <p>The "leak" of Panache active-record helpers is contained to this
 * adapter (per design doc §4.4): the entity-level
 * {@code findById}/{@code persist} calls live here only, not in any
 * application or domain code.
 *
 * <p>{@link #save} resolves insert-vs-update by id: if no entity exists
 * for the given PersonId a fresh entity is persisted; otherwise the
 * existing managed entity is updated in-place via
 * {@link PersonMapper#applyTo}. There are no UNIQUE constraints on the
 * person table, so no constraint translation is needed here (unlike the
 * programming context's episode number constraint).
 *
 * <p>{@link #findAll} returns a page of Persons ordered by
 * {@code created_at} ascending (oldest first).
 *
 * <p>Part of the People bounded context, infrastructure layer.
 */
@ApplicationScoped
public class PersonRepositoryImpl implements PersonRepository {

    private final PersonMapper mapper;

    /**
     * Creates the repository.
     *
     * @param mapper the entity ↔ aggregate translator
     */
    @Inject
    public PersonRepositoryImpl(PersonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Person> findById(PersonId id) {
        PersonEntity entity = PersonEntity.findById(id.value());
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public void save(Person person) {
        PersonEntity existing = PersonEntity.findById(person.id().value());
        if (existing == null) {
            PersonEntity entity = mapper.toEntity(person);
            entity.persist();
        } else {
            mapper.applyTo(person, existing);
            // Panache flushes the managed entity on transaction commit.
        }
    }

    @Override
    public List<Person> findAll(int page, int size) {
        return PersonEntity.<PersonEntity>findAll(
                io.quarkus.panache.common.Sort.ascending("createdAt"))
            .page(page, size)
            .list()
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
