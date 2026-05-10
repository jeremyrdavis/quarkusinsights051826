package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Use case: assign a Person as a speaker (guest) on an Episode.
 *
 * <p>Idempotent: re-assigning the same person is a no-op at the
 * aggregate level (the Episode does not record a second
 * SpeakerAssigned event), so this use case will publish zero events
 * on a duplicate assignment.
 *
 * <p>Part of the Programming bounded context, application layer.
 */
@ApplicationScoped
public class AssignSpeakerUseCase {

    private final EpisodeRepository episodeRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates an AssignSpeakerUseCase.
     *
     * @param episodeRepository the Episode repository port
     * @param eventPublisher    the domain event publisher port
     */
    @Inject
    public AssignSpeakerUseCase(EpisodeRepository episodeRepository,
                                 DomainEventPublisher eventPublisher) {
        this.episodeRepository = episodeRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Assigns the speaker.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not SCHEDULED or LIVE
     */
    @Transactional
    public void handle(AssignSpeakerCommand cmd) {
        Episode episode = episodeRepository.findById(cmd.episodeId())
            .orElseThrow(() -> new EpisodeNotFound(cmd.episodeId()));
        episode.assignSpeaker(cmd.personId());
        episodeRepository.save(episode);
        List<DomainEvent> events = episode.recordedEvents();
        episode.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
