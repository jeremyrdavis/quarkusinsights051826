package io.arrogantprogrammer.quarkusinsights.people.interfaces;

import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;

/**
 * Translates {@link Person} aggregates into wire-friendly
 * {@link PersonResponse}s for HTTP responses. Unwraps domain VOs
 * so no domain types leak across the JSON boundary.
 *
 * <p>Part of the People bounded context, interfaces layer.
 */
@ApplicationScoped
public class PersonResponseMapper {

    /**
     * Builds a flat {@link PersonResponse} from a Person.
     *
     * @param person the source aggregate; must not be null
     * @return the wire-shaped response
     */
    public PersonResponse toResponse(Person person) {
        PersonResponse.NameView nameView = new PersonResponse.NameView(
            person.name().first(),
            person.name().last(),
            person.name().displayName()
        );
        PersonResponse.SocialsView socialsView = new PersonResponse.SocialsView(
            person.socials().twitter().map(URI::toString).orElse(null),
            person.socials().linkedin().map(URI::toString).orElse(null),
            person.socials().website().map(URI::toString).orElse(null)
        );
        return new PersonResponse(
            person.id().value(),
            nameView,
            person.email().value(),
            person.bio().value(),
            socialsView,
            person.createdAt(),
            person.updatedAt()
        );
    }
}
