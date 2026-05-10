package io.arrogantprogrammer.quarkusinsights.people.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Person aggregate root — the source of truth for a host or guest
 * in the QuarkusInsights program.
 *
 * <p>A Person represents a real individual with a name, email address,
 * biographical description, and optional social links. There are no
 * Person subtypes (no Speaker, Presenter, etc.) — those are roles a
 * Person plays on a specific Episode, owned by the Programming context.
 *
 * <p><strong>Aggregate boundary.</strong> All Person state (name, bio,
 * socials) is owned by this aggregate. Cross-context references to a
 * Person are by {@link PersonId} only — never by direct Person reference.
 *
 * <p><strong>Domain events.</strong> Each behavior method that mutates
 * state appends a {@link DomainEvent} to the aggregate's internal
 * {@code recordedEvents} list. The application layer reads the list via
 * {@link #recordedEvents()} after persisting the aggregate, then calls
 * {@link #clearRecordedEvents()} to reset.
 *
 * <p><strong>Construction.</strong> The {@link #register} factory is the
 * only way to create a new Person through normal flow. {@link #rehydrate}
 * exists solely for the persistence adapter to reconstruct a Person from
 * stored state without recording an event; application code MUST NOT call
 * it.
 *
 * <p>Person has no lifecycle state machine — all behaviors are valid at
 * any time after registration.
 *
 * <p>Part of the People bounded context, domain layer.
 */
public class Person {

    private final PersonId id;
    private final Instant createdAt;
    private PersonName name;
    private Email email;
    private Bio bio;
    private SocialLinks socials;
    private Instant updatedAt;
    private final List<DomainEvent> recordedEvents = new ArrayList<>();

    private Person(PersonId id, PersonName name, Email email, Bio bio,
                   SocialLinks socials, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.socials = socials;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Registers a new Person and records a {@link PersonRegistered} domain
     * event.
     *
     * <p>The new Person's social links are initialized to
     * {@link SocialLinks#none()}; they can be set later via
     * {@link #updateSocials}.
     *
     * @param name  the person's display name; must not be null
     * @param email the person's email address; must not be null
     * @param bio   the person's biography; must not be null
     * @return a new Person with a freshly minted {@link PersonId}
     * @throws IllegalArgumentException if any parameter is null
     */
    public static Person register(PersonName name, Email email, Bio bio) {
        if (name == null) {
            throw new IllegalArgumentException("PersonName must not be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        if (bio == null) {
            throw new IllegalArgumentException("Bio must not be null");
        }
        Instant now = Instant.now();
        Person person = new Person(
            PersonId.random(), name, email, bio, SocialLinks.none(), now, now);
        person.recordedEvents.add(new PersonRegistered(person.id, name, email, now));
        return person;
    }

    /**
     * Reconstructs a Person from its persisted state.
     *
     * <p><strong>For persistence adapter use only.</strong> Application
     * code MUST NOT call this — use {@link #register} or load via the
     * {@link PersonRepository}. Rehydration does not record any domain
     * event because no domain action is occurring.
     *
     * @param id        the persisted id
     * @param name      the persisted name
     * @param email     the persisted email
     * @param bio       the persisted biography
     * @param socials   the persisted social links
     * @param createdAt the persisted creation timestamp
     * @param updatedAt the persisted last-update timestamp
     * @return a hydrated Person whose recordedEvents list is empty
     */
    public static Person rehydrate(PersonId id, PersonName name, Email email, Bio bio,
                                   SocialLinks socials, Instant createdAt, Instant updatedAt) {
        return new Person(id, name, email, bio, socials, createdAt, updatedAt);
    }

    /** @return the aggregate's identifier */
    public PersonId id() { return id; }

    /** @return the person's display name */
    public PersonName name() { return name; }

    /** @return the person's email address */
    public Email email() { return email; }

    /** @return the person's biography */
    public Bio bio() { return bio; }

    /** @return the person's social links */
    public SocialLinks socials() { return socials; }

    /** @return the instant the aggregate was created */
    public Instant createdAt() { return createdAt; }

    /** @return the instant of the last state-mutating behavior method */
    public Instant updatedAt() { return updatedAt; }

    /**
     * @return an unmodifiable snapshot of domain events recorded by
     *     this aggregate since the last {@link #clearRecordedEvents()}
     */
    public List<DomainEvent> recordedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recordedEvents));
    }

    /**
     * Clears the aggregate's recorded-events list. The application
     * layer calls this after publishing the events so subsequent
     * behavior calls do not redeliver the same events.
     */
    public void clearRecordedEvents() {
        recordedEvents.clear();
    }

    /**
     * Updates the person's name and records a {@link PersonRenamed} event.
     *
     * @param newName the new name; must not be null
     * @throws IllegalArgumentException if {@code newName} is null
     */
    public void rename(PersonName newName) {
        if (newName == null) {
            throw new IllegalArgumentException("PersonName must not be null");
        }
        Instant now = Instant.now();
        this.name = newName;
        this.updatedAt = now;
        recordedEvents.add(new PersonRenamed(id, newName, now));
    }

    /**
     * Updates the person's biography and records a {@link PersonBioUpdated}
     * event.
     *
     * @param newBio the new biography; must not be null
     * @throws IllegalArgumentException if {@code newBio} is null
     */
    public void updateBio(Bio newBio) {
        if (newBio == null) {
            throw new IllegalArgumentException("Bio must not be null");
        }
        Instant now = Instant.now();
        this.bio = newBio;
        this.updatedAt = now;
        recordedEvents.add(new PersonBioUpdated(id, now));
    }

    /**
     * Updates the person's social links and records a
     * {@link PersonSocialsUpdated} event.
     *
     * @param newSocials the new social links; must not be null
     * @throws IllegalArgumentException if {@code newSocials} is null
     */
    public void updateSocials(SocialLinks newSocials) {
        if (newSocials == null) {
            throw new IllegalArgumentException("SocialLinks must not be null");
        }
        Instant now = Instant.now();
        this.socials = newSocials;
        this.updatedAt = now;
        recordedEvents.add(new PersonSocialsUpdated(id, now));
    }
}
