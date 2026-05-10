package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumberAlreadyExists;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Use case: schedule a new Episode.
 *
 * <p>Loads no existing aggregate (this is a factory operation). It
 * checks number uniqueness against the repository, invokes
 * {@link Episode#schedule}, persists, and dispatches the recorded
 * {@code EpisodeScheduled} event.
 *
 * <p>Number uniqueness is a cross-aggregate invariant enforced here
 * at the application layer (see SPEC.md §3.6). The persistence
 * adapter additionally enforces it via a database UNIQUE constraint
 * (Plan 2C); the adapter translates the resulting
 * ConstraintViolationException to {@link EpisodeNumberAlreadyExists}
 * so the same domain exception is raised regardless of whether the
 * race lands at the application or persistence layer.
 *
 * <p>Part of the Programming bounded context, application layer.
 */
@ApplicationScoped
public class ScheduleEpisodeUseCase {

    private final EpisodeRepository episodeRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a ScheduleEpisodeUseCase.
     *
     * @param episodeRepository the Episode repository port
     * @param eventPublisher    the domain event publisher port
     */
    @Inject
    public ScheduleEpisodeUseCase(EpisodeRepository episodeRepository,
                                   DomainEventPublisher eventPublisher) {
        this.episodeRepository = episodeRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Schedules a new Episode and publishes the EpisodeScheduled
     * domain event.
     *
     * @param cmd the schedule command; must not be null
     * @return the id of the freshly scheduled Episode
     * @throws EpisodeNumberAlreadyExists if the requested number is
     *     already in use
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast
     *     if the air date is strictly before today (propagated from
     *     {@link Episode#schedule})
     */
    @Transactional
    public EpisodeId handle(ScheduleEpisodeCommand cmd) {
        episodeRepository.findByNumber(cmd.number()).ifPresent(existing -> {
            throw new EpisodeNumberAlreadyExists(cmd.number());
        });
        Episode episode = Episode.schedule(cmd.number(), cmd.title(), cmd.airDate());
        episodeRepository.save(episode);
        List<DomainEvent> events = episode.recordedEvents();
        episode.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
        return episode.id();
    }
}
