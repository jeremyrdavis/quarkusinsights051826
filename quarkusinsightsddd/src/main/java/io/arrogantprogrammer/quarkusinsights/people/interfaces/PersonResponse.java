package io.arrogantprogrammer.quarkusinsights.people.interfaces;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire-shaped JSON response for a Person aggregate.
 *
 * <p>Designed for direct Jackson serialization — all components are
 * primitives or basic JDK types; no domain types leak across the wire.
 * {@link PersonResponseMapper} produces these from a loaded
 * {@code Person} aggregate.
 *
 * <p>Part of the People bounded context, interfaces layer.
 *
 * @param id        the person's unique identifier
 * @param name      the person's name (first, last, display)
 * @param email     the person's email address
 * @param bio       the person's biography
 * @param socials   the person's social links (nullable per link)
 * @param createdAt when the Person was registered
 * @param updatedAt when the Person was last mutated
 */
public record PersonResponse(
    UUID id,
    NameView name,
    String email,
    String bio,
    SocialsView socials,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Inline view of a PersonName Value Object.
     *
     * @param first       given name
     * @param last        family name
     * @param displayName pre-formatted "first last"
     */
    public record NameView(String first, String last, String displayName) {
    }

    /**
     * Inline view of a SocialLinks Value Object.
     *
     * <p>Each field is nullable when the corresponding Optional is empty.
     *
     * @param twitter  Twitter/X profile URI string, or null if absent
     * @param linkedin LinkedIn profile URI string, or null if absent
     * @param website  website URI string, or null if absent
     */
    public record SocialsView(String twitter, String linkedin, String website) {
    }
}
