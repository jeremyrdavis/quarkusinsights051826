package io.arrogantprogrammer.quarkusinsights.people.domain;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link SocialLinks} value-object validation and behavior.
 */
class SocialLinksTest {

    @Test
    void nullTwitterOptionalThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new SocialLinks(null, Optional.empty(), Optional.empty()));
    }

    @Test
    void nullLinkedinOptionalThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new SocialLinks(Optional.empty(), null, Optional.empty()));
    }

    @Test
    void nullWebsiteOptionalThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new SocialLinks(Optional.empty(), Optional.empty(), null));
    }

    @Test
    void allEmptyViaConstructorAccepted() {
        SocialLinks links = new SocialLinks(
            Optional.empty(), Optional.empty(), Optional.empty());
        assertFalse(links.twitter().isPresent());
        assertFalse(links.linkedin().isPresent());
        assertFalse(links.website().isPresent());
    }

    @Test
    void noneFactoryReturnsAllEmpty() {
        SocialLinks links = SocialLinks.none();
        assertFalse(links.twitter().isPresent());
        assertFalse(links.linkedin().isPresent());
        assertFalse(links.website().isPresent());
    }

    @Test
    void equalityByValue() {
        SocialLinks a = SocialLinks.none();
        SocialLinks b = SocialLinks.none();
        assertEquals(a, b);
    }

    @Test
    void roundTripWithValidUris() {
        URI twitterUri = URI.create("https://twitter.com/jsmith");
        URI linkedinUri = URI.create("https://linkedin.com/in/jsmith");
        URI websiteUri = URI.create("https://jsmith.dev");

        SocialLinks links = new SocialLinks(
            Optional.of(twitterUri),
            Optional.of(linkedinUri),
            Optional.of(websiteUri)
        );

        assertTrue(links.twitter().isPresent());
        assertEquals(twitterUri, links.twitter().get());
        assertTrue(links.linkedin().isPresent());
        assertEquals(linkedinUri, links.linkedin().get());
        assertTrue(links.website().isPresent());
        assertEquals(websiteUri, links.website().get());
    }
}
