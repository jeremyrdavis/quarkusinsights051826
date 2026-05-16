package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.LocalDate;

/**
 * Flat read-only row for the admin Episodes list. Carries the subset
 * of state the list view renders: identification, lifecycle status,
 * counts of presenters and speakers, and whether an abstract has been
 * submitted. Heavier than {@link EpisodeSummary} (which is a
 * cross-context read DTO) — this row exists specifically for the
 * admin list and is not consumed across contexts.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param id              the episode's id
 * @param number          the episode's sequential number
 * @param title           the display title
 * @param airDate         the air date
 * @param status          the current lifecycle status
 * @param presenterCount  count of assigned presenters
 * @param speakerCount    count of assigned speakers
 * @param hasAbstract     whether an abstract has been submitted
 */
public record EpisodeListRow(
    EpisodeId id,
    int number,
    String title,
    LocalDate airDate,
    EpisodeStatus status,
    int presenterCount,
    int speakerCount,
    boolean hasAbstract
) {
    public EpisodeListRow {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (airDate == null) throw new IllegalArgumentException("airDate must not be null");
        if (status == null) throw new IllegalArgumentException("status must not be null");
    }
}
