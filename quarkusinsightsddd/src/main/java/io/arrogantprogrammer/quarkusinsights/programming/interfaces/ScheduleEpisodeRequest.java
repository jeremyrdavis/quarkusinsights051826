package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import java.time.LocalDate;

/**
 * Wire DTO for {@code POST /episodes}. Translated to a domain
 * {@code ScheduleEpisodeCommand} by the resource layer using the
 * domain VO constructors (which validate format).
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 *
 * @param number  the desired episode number (≥ 1; validated when wrapped in EpisodeNumber)
 * @param title   the display title (1..200 chars; validated by EpisodeTitle)
 * @param airDate the air date (today or later; precondition checked at the aggregate)
 */
public record ScheduleEpisodeRequest(int number, String title, LocalDate airDate) {
}
