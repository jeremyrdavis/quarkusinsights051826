package io.arrogantprogrammer.quarkusinsights.catalog.domain;

import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Read-model summary of a person who hosts (presents) an episode, as seen from
 * the Public Catalog bounded context.
 *
 * <p>Part of the Public Catalog bounded context, domain layer.
 *
 * <p>This is a distinct type from {@link SpeakerSummary} even though both carry
 * the same fields. The distinction matters at the catalog level: a Presenter is
 * the host who runs the show, while a Speaker is a guest who appears on it.
 * Keeping them as separate types prevents accidental confusion in the catalog's
 * read-model assembler and projectors, and clarifies intent when reading or
 * rendering view rows.
 *
 * <p>Both components are mandatory — a presenter without an identity or a
 * display name cannot meaningfully appear on the public catalog page.
 *
 * @param id          the person's unique identifier; must not be null
 * @param displayName the person's display name in "first last" form; must not be null or blank
 */
public record PresenterSummary(PersonId id, String displayName) {

    /**
     * Creates a PresenterSummary, validating that neither component is null.
     *
     * @param id          the person's identifier; must not be null
     * @param displayName the display name; must not be null or blank
     * @throws IllegalArgumentException if either component is null or displayName is blank
     */
    public PresenterSummary {
        if (id == null) {
            throw new IllegalArgumentException("PresenterSummary.id must not be null");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("PresenterSummary.displayName must not be null or blank");
        }
    }
}
