package io.arrogantprogrammer.quarkusinsights.people.application;

import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonNotFound;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.arrogantprogrammer.quarkusinsights.shared.application.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Application service for the Person aggregate.
 *
 * <p>Orchestrates the four Person operations exposed to clients:
 * registration, rename, bio update, and social links update. Each method
 * is a transactional unit of work that loads (or creates) the aggregate,
 * invokes its behavior, persists, and dispatches the recorded domain
 * events.
 *
 * <p>The service contains no business rules — those live in the
 * {@link Person} aggregate. All cross-aggregate invariants for the People
 * context are checked here (currently none exist beyond what the aggregate
 * itself enforces).
 *
 * <p>Domain events are read from the aggregate after a successful save,
 * dispatched via {@link DomainEventPublisher}, and then cleared from the
 * aggregate via {@link Person#clearRecordedEvents()} to prevent
 * redelivery on subsequent calls.
 *
 * <p>Part of the People bounded context, application layer.
 */
@ApplicationScoped
public class PersonService {

    private final PersonRepository personRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates the service.
     *
     * @param personRepository the Person repository port
     * @param eventPublisher   the domain event publisher port
     */
    @Inject
    public PersonService(PersonRepository personRepository,
                         DomainEventPublisher eventPublisher) {
        this.personRepository = personRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers a new Person and publishes the {@code PersonRegistered}
     * domain event.
     *
     * @param cmd the registration command; must not be null
     * @return the freshly minted {@link PersonId} of the new Person
     * @throws IllegalArgumentException if {@code cmd} is null or any
     *     Value Object in the command fails its own validation
     */
    @Transactional
    public PersonId register(RegisterPersonCommand cmd) {
        Person person = Person.register(cmd.name(), cmd.email(), cmd.bio());
        personRepository.save(person);
        dispatchEvents(person);
        return person.id();
    }

    /**
     * Renames an existing Person and publishes the {@code PersonRenamed}
     * domain event.
     *
     * @param cmd the rename command; must not be null
     * @throws PersonNotFound if no Person with the given id exists
     * @throws IllegalArgumentException if the new name fails VO validation
     */
    @Transactional
    public void rename(RenamePersonCommand cmd) {
        Person person = loadOrThrow(cmd.personId());
        person.rename(cmd.name());
        personRepository.save(person);
        dispatchEvents(person);
    }

    /**
     * Updates an existing Person's biography and publishes the
     * {@code PersonBioUpdated} domain event.
     *
     * @param cmd the update command; must not be null
     * @throws PersonNotFound if no Person with the given id exists
     * @throws IllegalArgumentException if the new bio fails VO validation
     */
    @Transactional
    public void updateBio(UpdateBioCommand cmd) {
        Person person = loadOrThrow(cmd.personId());
        person.updateBio(cmd.bio());
        personRepository.save(person);
        dispatchEvents(person);
    }

    /**
     * Updates an existing Person's social links and publishes the
     * {@code PersonSocialsUpdated} domain event.
     *
     * @param cmd the update command; must not be null
     * @throws PersonNotFound if no Person with the given id exists
     * @throws IllegalArgumentException if the new social links fail VO
     *     validation
     */
    @Transactional
    public void updateSocials(UpdateSocialsCommand cmd) {
        Person person = loadOrThrow(cmd.personId());
        person.updateSocials(cmd.socials());
        personRepository.save(person);
        dispatchEvents(person);
    }

    private Person loadOrThrow(PersonId id) {
        return personRepository.findById(id)
            .orElseThrow(() -> new PersonNotFound(id));
    }

    private void dispatchEvents(Person person) {
        List<DomainEvent> events = person.recordedEvents();
        person.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
