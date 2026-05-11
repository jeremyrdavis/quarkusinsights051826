package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.Optional;

/**
 * Flat read-only projection of an Episode aggregate, returned by
 * {@link EpisodeQueries}. Carries the subset of state that
 * cross-context consumers (notably the Public Catalog's projectors)
 * need to enrich their denormalized views.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param id           the episode's id
 * @param number       the episode's sequential number
 * @param title        the display title
 * @param airDate      the air date
 * @param status       the current lifecycle status
 * @param abstractText the submitted abstract text, or empty if not yet submitted
 */
public record EpisodeSummary(
    EpisodeId id,
    EpisodeNumber number,
    EpisodeTitle title,
    AirDate airDate,
    EpisodeStatus status,
    Optional<AbstractText> abstractText
) {
    /**
     * Creates an EpisodeSummary.
     *
     * @throws IllegalArgumentException if any required component is null
     */
    public EpisodeSummary {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (number == null) throw new IllegalArgumentException("number must not be null");
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (airDate == null) throw new IllegalArgumentException("airDate must not be null");
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (abstractText == null) throw new IllegalArgumentException("abstractText Optional must not be null (use Optional.empty())");
    }
}
