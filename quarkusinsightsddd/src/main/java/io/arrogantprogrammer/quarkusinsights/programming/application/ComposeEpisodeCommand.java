package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.util.Set;

/**
 * Command to schedule a new Episode and populate it with its initial
 * content in a single transactional step: title, number, air date,
 * abstract, presenters, and speakers.
 *
 * <p>Consumed by {@link EpisodeService#compose}. The use case enforces
 * cross-aggregate episode-number uniqueness, then invokes
 * {@link io.arrogantprogrammer.quarkusinsights.programming.domain.Episode#schedule},
 * {@code submitAbstract}, {@code assignPresenter} for each presenter id,
 * and {@code assignSpeaker} for each speaker id, persisting and
 * dispatching all recorded events as a unit. Any failure rolls back the
 * entire composition.
 *
 * <p>The presenter and speaker sets MUST each contain at least one id —
 * a composed Episode is intended to be publishable, and an Episode
 * cannot reach {@code PUBLISHED} without at least one presenter and one
 * speaker. Callers that need a partially-populated Episode should use
 * the separate {@link ScheduleEpisodeCommand} flow.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param number       the desired episode number; must not be null
 * @param title        the display title; must not be null
 * @param airDate      the air date; must not be null
 * @param abstractText the abstract text; must not be null
 * @param presenters   non-empty set of presenter PersonIds; must not be null
 * @param speakers     non-empty set of speaker PersonIds; must not be null
 */
public record ComposeEpisodeCommand(
    EpisodeNumber number,
    EpisodeTitle title,
    AirDate airDate,
    AbstractText abstractText,
    Set<PersonId> presenters,
    Set<PersonId> speakers
) {

    /**
     * Creates a ComposeEpisodeCommand.
     *
     * @throws IllegalArgumentException if any component is null, or if
     *     either the presenters set or speakers set is empty
     */
    public ComposeEpisodeCommand {
        if (number == null) throw new IllegalArgumentException("number must not be null");
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (airDate == null) throw new IllegalArgumentException("airDate must not be null");
        if (abstractText == null) throw new IllegalArgumentException("abstractText must not be null");
        if (presenters == null) throw new IllegalArgumentException("presenters must not be null");
        if (speakers == null) throw new IllegalArgumentException("speakers must not be null");
        if (presenters.isEmpty()) {
            throw new IllegalArgumentException("compose requires at least one presenter");
        }
        if (speakers.isEmpty()) {
            throw new IllegalArgumentException("compose requires at least one speaker");
        }
        presenters = Set.copyOf(presenters);
        speakers = Set.copyOf(speakers);
    }
}
