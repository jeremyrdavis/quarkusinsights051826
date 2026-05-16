package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Wire DTO for {@code POST /api/episodes/compose}. Translated to a
 * domain {@link io.arrogantprogrammer.quarkusinsights.programming.application.ComposeEpisodeCommand}
 * by the resource layer using domain VO constructors (which validate
 * format).
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 *
 * @param number       the desired episode number (≥ 1; validated by EpisodeNumber)
 * @param title        the display title (1..200 chars; validated by EpisodeTitle)
 * @param airDate      the air date (today or later; checked at the aggregate)
 * @param abstractText the abstract body (100..5000 chars; validated by AbstractText)
 * @param presenterIds non-empty list of presenter UUIDs
 * @param speakerIds   non-empty list of speaker UUIDs
 */
public record ComposeEpisodeRequest(
    int number,
    String title,
    LocalDate airDate,
    String abstractText,
    List<UUID> presenterIds,
    List<UUID> speakerIds
) {
}
