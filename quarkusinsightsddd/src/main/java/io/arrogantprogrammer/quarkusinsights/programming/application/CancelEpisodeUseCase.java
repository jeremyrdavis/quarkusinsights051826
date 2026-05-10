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
 * Use case: cancel a SCHEDULED Episode.
 *
 * <p>Loads the Episode, invokes {@link Episode#cancel(String)},
 * persists, and dispatches the recorded {@code EpisodeCanceled}
 * event with the supplied reason. Cancellation is only permitted
 * before the Episode goes live.
 *
 * <p>Part of the Programming bounded context, application layer.
 */
@ApplicationScoped
public class CancelEpisodeUseCase {

    private final EpisodeRepository episodeRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a CancelEpisodeUseCase.
     *
     * @param episodeRepository the Episode repository port
     * @param eventPublisher    the domain event publisher port
     */
    @Inject
    public CancelEpisodeUseCase(EpisodeRepository episodeRepository,
                                 DomainEventPublisher eventPublisher) {
        this.episodeRepository = episodeRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Cancels the Episode.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not SCHEDULED
     */
    @Transactional
    public void handle(CancelEpisodeCommand cmd) {
        Episode episode = episodeRepository.findById(cmd.episodeId())
            .orElseThrow(() -> new EpisodeNotFound(cmd.episodeId()));
        episode.cancel(cmd.reason());
        episodeRepository.save(episode);
        List<DomainEvent> events = episode.recordedEvents();
        episode.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
