package io.arrogantprogrammer.quarkusinsights.people.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.people.domain.SocialLinks;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies {@link PersonMapper} round-trips a Person aggregate through
 * {@link PersonEntity} preserving all state. Pure JUnit; no Quarkus
 * startup since the mapper has no CDI dependencies of its own.
 */
class PersonMapperTest {

    private PersonMapper mapper;

    private static final PersonName NAME = new PersonName("Holly", "Cummins");
    private static final Email EMAIL = new Email("holly@example.com");
    private static final Bio BIO = new Bio("A".repeat(50));

    @BeforeEach
    void setUp() {
        mapper = new PersonMapper();
    }

    private Person newPerson() {
        return Person.register(NAME, EMAIL, BIO);
    }

    @Test
    void roundTripWithNoSocialsPreservesAllState() {
        Person original = newPerson();
        original.clearRecordedEvents();

        PersonEntity entity = mapper.toEntity(original);
        Person roundTripped = mapper.toDomain(entity);

        assertEquals(original.id(), roundTripped.id());
        assertEquals(original.name(), roundTripped.name());
        assertEquals(original.email(), roundTripped.email());
        assertEquals(original.bio(), roundTripped.bio());
        assertEquals(SocialLinks.none(), roundTripped.socials());
        assertNotNull(roundTripped.createdAt());
        assertNotNull(roundTripped.updatedAt());
    }

    @Test
    void roundTripWithAllThreeSocialsPreservesThem() {
        Person original = newPerson();
        SocialLinks socials = new SocialLinks(
            Optional.of(URI.create("https://twitter.com/holly")),
            Optional.of(URI.create("https://linkedin.com/in/holly")),
            Optional.of(URI.create("https://holly.dev"))
        );
        original.updateSocials(socials);
        original.clearRecordedEvents();

        PersonEntity entity = mapper.toEntity(original);
        Person roundTripped = mapper.toDomain(entity);

        assertEquals("https://twitter.com/holly",
            roundTripped.socials().twitter().map(URI::toString).orElse(null));
        assertEquals("https://linkedin.com/in/holly",
            roundTripped.socials().linkedin().map(URI::toString).orElse(null));
        assertEquals("https://holly.dev",
            roundTripped.socials().website().map(URI::toString).orElse(null));
    }

    @Test
    void roundTripWithPartialSocialsPreservesPresenceAndAbsence() {
        Person original = newPerson();
        SocialLinks partial = new SocialLinks(
            Optional.of(URI.create("https://twitter.com/holly")),
            Optional.empty(),
            Optional.empty()
        );
        original.updateSocials(partial);
        original.clearRecordedEvents();

        PersonEntity entity = mapper.toEntity(original);

        assertNotNull(entity.socialTwitter);
        assertNull(entity.socialLinkedin);
        assertNull(entity.socialWebsite);

        Person roundTripped = mapper.toDomain(entity);
        assertEquals("https://twitter.com/holly",
            roundTripped.socials().twitter().map(URI::toString).orElse(null));
        assertEquals(Optional.empty(), roundTripped.socials().linkedin());
        assertEquals(Optional.empty(), roundTripped.socials().website());
    }

    @Test
    void applyToPreservesIdAndCreatedAtWhileUpdatingMutableFields() {
        Person original = newPerson();
        original.clearRecordedEvents();

        PersonEntity entity = mapper.toEntity(original);
        PersonId originalId = new PersonId(entity.id);
        long originalVersion = entity.version;

        // Mutate the aggregate
        PersonName newName = new PersonName("James", "Cummins");
        original.rename(newName);
        Bio newBio = new Bio("B".repeat(60));
        original.updateBio(newBio);

        mapper.applyTo(original, entity);

        // Identity and creation time preserved
        assertEquals(originalId.value(), entity.id);
        // Version is not touched by applyTo — JPA manages it
        assertEquals(originalVersion, entity.version);
        // Mutable fields updated
        assertEquals("James", entity.nameFirst);
        assertEquals("Cummins", entity.nameLast);
        assertEquals("B".repeat(60), entity.bio);
        assertNotNull(entity.updatedAt);
    }
}
