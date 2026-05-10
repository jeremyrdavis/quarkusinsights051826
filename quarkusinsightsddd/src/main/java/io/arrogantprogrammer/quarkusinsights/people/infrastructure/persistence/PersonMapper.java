package io.arrogantprogrammer.quarkusinsights.people.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.people.domain.SocialLinks;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.util.Optional;

/**
 * Translates between the {@link Person} domain aggregate and the
 * {@link PersonEntity} JPA persistence model.
 *
 * <p>The mapper is the single bridge between the two worlds: the
 * domain knows nothing about JPA, and the entity knows nothing about
 * domain methods. Construction direction is asymmetric:
 * <ul>
 *   <li>{@link #toEntity}: produces a fresh {@code PersonEntity}
 *       from a Person, suitable for {@code persist()}.</li>
 *   <li>{@link #toDomain}: produces a fresh {@link Person} from a
 *       loaded entity using {@link Person#rehydrate} (does not record
 *       any domain events — load is not a domain action).</li>
 *   <li>{@link #applyTo}: copies a Person's mutable state onto an
 *       already-managed entity, preserving JPA-managed fields
 *       ({@code @Id}, {@code @Version}, {@code createdAt}). Used on
 *       update.</li>
 * </ul>
 *
 * <p>SocialLinks ↔ entity columns: each {@code Optional<URI>} is stored
 * as a nullable {@code String} (via {@code URI.toString}) and
 * reconstructed via {@code Optional.ofNullable(s).map(URI::create)}.
 *
 * <p>Part of the People bounded context, infrastructure layer.
 */
@ApplicationScoped
public class PersonMapper {

    /**
     * Builds a fresh {@link PersonEntity} mirroring the given
     * {@link Person}. Used when persisting a new aggregate.
     *
     * @param person the source aggregate; must not be null
     * @return a fresh entity with all fields populated
     */
    public PersonEntity toEntity(Person person) {
        PersonEntity entity = new PersonEntity();
        entity.id = person.id().value();
        entity.nameFirst = person.name().first();
        entity.nameLast = person.name().last();
        entity.email = person.email().value();
        entity.bio = person.bio().value();
        entity.socialTwitter = person.socials().twitter().map(URI::toString).orElse(null);
        entity.socialLinkedin = person.socials().linkedin().map(URI::toString).orElse(null);
        entity.socialWebsite = person.socials().website().map(URI::toString).orElse(null);
        entity.createdAt = person.createdAt();
        entity.updatedAt = person.updatedAt();
        return entity;
    }

    /**
     * Reconstructs a {@link Person} aggregate from its persisted
     * state. Uses {@link Person#rehydrate} so no domain events are
     * recorded — load is not a domain action.
     *
     * @param entity the loaded entity; must not be null
     * @return the rehydrated Person
     */
    public Person toDomain(PersonEntity entity) {
        SocialLinks socials = new SocialLinks(
            Optional.ofNullable(entity.socialTwitter).map(URI::create),
            Optional.ofNullable(entity.socialLinkedin).map(URI::create),
            Optional.ofNullable(entity.socialWebsite).map(URI::create)
        );
        return Person.rehydrate(
            new PersonId(entity.id),
            new PersonName(entity.nameFirst, entity.nameLast),
            new Email(entity.email),
            new Bio(entity.bio),
            socials,
            entity.createdAt,
            entity.updatedAt
        );
    }

    /**
     * Copies the mutable state of a {@link Person} onto an
     * already-managed {@link PersonEntity}, preserving the entity's
     * identity ({@code @Id}), creation timestamp ({@code createdAt}),
     * and optimistic lock ({@code @Version}).
     *
     * <p>Used when persisting updates to an aggregate that was loaded,
     * mutated, and now needs flushing. Mutable fields updated: name,
     * email, bio, social links, and {@code updatedAt}.
     *
     * @param person the source aggregate (post-mutation)
     * @param entity the loaded entity to update in place
     */
    public void applyTo(Person person, PersonEntity entity) {
        entity.nameFirst = person.name().first();
        entity.nameLast = person.name().last();
        entity.email = person.email().value();
        entity.bio = person.bio().value();
        entity.socialTwitter = person.socials().twitter().map(URI::toString).orElse(null);
        entity.socialLinkedin = person.socials().linkedin().map(URI::toString).orElse(null);
        entity.socialWebsite = person.socials().website().map(URI::toString).orElse(null);
        entity.updatedAt = person.updatedAt();
    }
}
