package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;

/**
 * Command to schedule a new Episode.
 *
 * <p>Consumed by {@code ScheduleEpisodeUseCase}. The use case loads no
 * existing aggregate (this is a factory operation); it checks number
 * uniqueness via the repository and then invokes
 * {@link io.arrogantprogrammer.quarkusinsights.programming.domain.Episode#schedule}.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param number  the desired episode number; must not be null
 * @param title   the display title; must not be null
 * @param airDate the air date; must not be null
 */
public record ScheduleEpisodeCommand(EpisodeNumber number, EpisodeTitle title, AirDate airDate) {

    /**
     * Creates a ScheduleEpisodeCommand.
     *
     * @throws IllegalArgumentException if any component is null
     */
    public ScheduleEpisodeCommand {
        if (number == null) throw new IllegalArgumentException("number must not be null");
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (airDate == null) throw new IllegalArgumentException("airDate must not be null");
    }
}
