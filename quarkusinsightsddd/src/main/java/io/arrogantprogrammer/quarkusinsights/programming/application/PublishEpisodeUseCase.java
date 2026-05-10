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
 * Use case: publish a LIVE Episode.
 *
 * <p>Loads the Episode, invokes {@link Episode#publish()}, persists,
 * and dispatches the recorded {@code EpisodePublished} event.
 *
 * <p>The aggregate's {@code publish()} enforces four preconditions:
 * the Episode must be LIVE, must have an abstract, must have at
 * least one presenter, and must have at least one speaker. This use
 * case propagates each of those exceptions unchanged; the REST
 * adapter (Plan 2C) maps them to 409 Conflict.
 *
 * <p>Part of the Programming bounded context, application layer.
 */
@ApplicationScoped
public class PublishEpisodeUseCase {

    private final EpisodeRepository episodeRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a PublishEpisodeUseCase.
     *
     * @param episodeRepository the Episode repository port
     * @param eventPublisher    the domain event publisher port
     */
    @Inject
    public PublishEpisodeUseCase(EpisodeRepository episodeRepository,
                                  DomainEventPublisher eventPublisher) {
        this.episodeRepository = episodeRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes the Episode.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not LIVE
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.MissingAbstract
     *     if no abstract has been submitted
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.MissingPresenter
     *     if no presenters have been assigned
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.MissingSpeaker
     *     if no speakers have been assigned
     */
    @Transactional
    public void handle(PublishEpisodeCommand cmd) {
        Episode episode = episodeRepository.findById(cmd.episodeId())
            .orElseThrow(() -> new EpisodeNotFound(cmd.episodeId()));
        episode.publish();
        episodeRepository.save(episode);
        List<DomainEvent> events = episode.recordedEvents();
        episode.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
