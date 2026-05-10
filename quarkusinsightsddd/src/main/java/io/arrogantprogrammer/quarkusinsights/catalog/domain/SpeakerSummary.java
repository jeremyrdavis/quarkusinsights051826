package io.arrogantprogrammer.quarkusinsights.catalog.domain;

import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Read-model summary of a person who appears as a guest speaker on an episode,
 * as seen from the Public Catalog bounded context.
 *
 * <p>Part of the Public Catalog bounded context, domain layer.
 *
 * <p>This is a distinct type from {@link PresenterSummary} even though both
 * carry the same fields. Keeping them as separate types prevents accidental
 * confusion when assembling or rendering the catalog view: a Speaker is a
 * guest invited by the Presenter, and their respective lists are displayed in
 * different sections of the public episode page.
 *
 * <p>Both components are mandatory — a speaker without an identity or a display
 * name cannot meaningfully appear on the public catalog page.
 *
 * @param id          the person's unique identifier; must not be null
 * @param displayName the person's display name in "first last" form; must not be null or blank
 */
public record SpeakerSummary(PersonId id, String displayName) {

    /**
     * Creates a SpeakerSummary, validating that neither component is null.
     *
     * @param id          the person's identifier; must not be null
     * @param displayName the display name; must not be null or blank
     * @throws IllegalArgumentException if either component is null or displayName is blank
     */
    public SpeakerSummary {
        if (id == null) {
            throw new IllegalArgumentException("SpeakerSummary.id must not be null");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("SpeakerSummary.displayName must not be null or blank");
        }
    }
}
