package io.arrogantprogrammer.quarkusinsights.people.domain;

import java.net.URI;
import java.util.Optional;

/**
 * Social media and web presence links for a Person — an immutable Value
 * Object.
 *
 * <p>Each link is optional because not every person maintains a presence
 * on every platform. Callers that have no link for a given platform MUST
 * pass {@link Optional#empty()} rather than {@code null}; null Optionals
 * are rejected to enforce consistent handling throughout the domain.
 *
 * <p>Use {@link #none()} to construct a SocialLinks with all links absent.
 *
 * <p>Part of the People bounded context, domain layer.
 *
 * @param twitter  the person's Twitter/X profile URI, or empty if absent
 * @param linkedin the person's LinkedIn profile URI, or empty if absent
 * @param website  the person's personal or professional website URI,
 *                 or empty if absent
 */
public record SocialLinks(Optional<URI> twitter, Optional<URI> linkedin, Optional<URI> website) {

    /**
     * Creates a SocialLinks.
     *
     * @param twitter  Twitter URI wrapped in an Optional; must not be null
     *                 (pass {@link Optional#empty()} for absent)
     * @param linkedin LinkedIn URI wrapped in an Optional; must not be null
     * @param website  website URI wrapped in an Optional; must not be null
     * @throws IllegalArgumentException if any Optional argument is null
     */
    public SocialLinks {
        if (twitter == null) {
            throw new IllegalArgumentException("SocialLinks.twitter must not be null; use Optional.empty() for absent values");
        }
        if (linkedin == null) {
            throw new IllegalArgumentException("SocialLinks.linkedin must not be null; use Optional.empty() for absent values");
        }
        if (website == null) {
            throw new IllegalArgumentException("SocialLinks.website must not be null; use Optional.empty() for absent values");
        }
    }

    /**
     * Returns a SocialLinks with all links absent.
     *
     * <p>This is the canonical default for a newly registered Person before
     * their social presence is populated.
     *
     * @return a SocialLinks with all three Optionals empty
     */
    public static SocialLinks none() {
        return new SocialLinks(Optional.empty(), Optional.empty(), Optional.empty());
    }
}
