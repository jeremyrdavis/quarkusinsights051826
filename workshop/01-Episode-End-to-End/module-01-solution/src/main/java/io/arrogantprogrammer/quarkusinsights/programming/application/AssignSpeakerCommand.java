package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

/**
 * Command to assign a Person as a speaker (guest) on an Episode.
 *
 * <p>Consumed by {@code AssignSpeakerUseCase}. Idempotent at the
 * aggregate layer: assigning the same Person twice does not record a
 * second {@code SpeakerAssigned} event.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param episodeId the target Episode's id; must not be null
 * @param personId  the Person being assigned; must not be null
 */
public record AssignSpeakerCommand(EpisodeId episodeId, PersonId personId) {

    /**
     * @throws IllegalArgumentException if either component is null
     */
    public AssignSpeakerCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
        if (personId == null) throw new IllegalArgumentException("personId must not be null");
    }
}
