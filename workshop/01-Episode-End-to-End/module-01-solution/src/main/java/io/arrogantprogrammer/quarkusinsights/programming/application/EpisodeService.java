package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumberAlreadyExists;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.application.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Application service for the Episode aggregate.
 *
 * <p>Orchestrates the seven Episode operations exposed to clients:
 * scheduling, abstract submission, presenter/speaker assignment,
 * lifecycle transitions (go live / publish / cancel). Each method is
 * a transactional unit of work that loads (or creates) the
 * aggregate, invokes its behavior, persists, and dispatches the
 * recorded domain events.
 *
 * <p>The service contains no business rules — those live in the
 * {@link Episode} aggregate. The service contains workflow plus the
 * cross-aggregate invariants that no single Episode can verify
 * (notably the {@link io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber} uniqueness check in
 * {@link #schedule}).
 *
 * <p>Part of the Programming bounded context, application layer.
 */
@ApplicationScoped
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates the service.
     *
     * @param episodeRepository the Episode repository port
     * @param eventPublisher    the domain event publisher port
     */
    @Inject
    public EpisodeService(EpisodeRepository episodeRepository,
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
     *     already in use (cross-aggregate invariant; SPEC.md §3.6,
     *     additionally backed by a database UNIQUE constraint)
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast
     *     if the air date is strictly before today
     */
    @Transactional
    public EpisodeId schedule(ScheduleEpisodeCommand cmd) {
        episodeRepository.findByNumber(cmd.number()).ifPresent(existing -> {
            throw new EpisodeNumberAlreadyExists(cmd.number());
        });
        Episode episode = Episode.schedule(cmd.number(), cmd.title(), cmd.airDate());
        episodeRepository.save(episode);
        dispatchEvents(episode);
        return episode.id();
    }

    /**
     * Submits or replaces the Episode's abstract.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not in SCHEDULED status
     */
    @Transactional
    public void submitAbstract(SubmitAbstractCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.submitAbstract(cmd.text());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    /**
     * Assigns a Person as a presenter on the Episode.
     *
     * <p>Idempotent: re-assigning the same person publishes zero
     * events (the aggregate does not record a duplicate).
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not SCHEDULED or LIVE
     */
    @Transactional
    public void assignPresenter(AssignPresenterCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.assignPresenter(cmd.personId());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    /**
     * Assigns a Person as a speaker on the Episode.
     *
     * <p>Idempotent: re-assigning the same person publishes zero
     * events.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not SCHEDULED or LIVE
     */
    @Transactional
    public void assignSpeaker(AssignSpeakerCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.assignSpeaker(cmd.personId());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    /**
     * Transitions the Episode from SCHEDULED to LIVE.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not SCHEDULED
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast
     *     if today's date is strictly before the air date
     */
    @Transactional
    public void goLive(GoLiveCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.goLive();
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    /**
     * Publishes a LIVE Episode.
     *
     * <p>The aggregate's {@code publish()} enforces four
     * preconditions: status must be LIVE, abstract must be present,
     * at least one presenter, at least one speaker. This method
     * propagates each precondition exception unchanged; the REST
     * adapter maps them to 409 Conflict.
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
    public void publish(PublishEpisodeCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.publish();
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    /**
     * Cancels a SCHEDULED Episode with a reason. Cancellation is
     * only permitted before the Episode goes live.
     *
     * @param cmd the command; must not be null
     * @throws EpisodeNotFound if no Episode with the given id exists
     * @throws io.arrogantprogrammer.quarkusinsights.programming.domain.IllegalEpisodeTransition
     *     if the Episode is not SCHEDULED
     */
    @Transactional
    public void cancel(CancelEpisodeCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.cancel(cmd.reason());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    private Episode loadOrThrow(EpisodeId id) {
        return episodeRepository.findById(id)
            .orElseThrow(() -> new EpisodeNotFound(id));
    }

    private void dispatchEvents(Episode episode) {
        List<DomainEvent> events = episode.recordedEvents();
        episode.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
