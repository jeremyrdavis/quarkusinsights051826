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
 * Use case: transition an Episode from SCHEDULED to LIVE.
 *
 * <p>Loads the Episode, invokes {@link Episode#goLive()}, persists,
 * and dispatches the recorded {@code EpisodeWentLive} event.
 *
 * <p>Part of the Programming bounded context, application layer.
 */
@ApplicationScoped
public class GoLiveUseCase {

    private final EpisodeRepository episodeRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a GoLiveUseCase.
     *
     * @param episodeRepository the Episode repository port
     * @param eventPublisher    the domain event publisher port
     */
    @Inject
    public GoLiveUseCase(EpisodeRepository episodeRepository,
                         DomainEventPublisher eventPublisher) {
        this.episodeRepository = episodeRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Transitions the Episode to LIVE.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not SCHEDULED
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast
     *     if today's date is strictly before the air date
     */
    @Transactional
    public void handle(GoLiveCommand cmd) {
        Episode episode = episodeRepository.findById(cmd.episodeId())
            .orElseThrow(() -> new EpisodeNotFound(cmd.episodeId()));
        episode.goLive();
        episodeRepository.save(episode);
        List<DomainEvent> events = episode.recordedEvents();
        episode.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
