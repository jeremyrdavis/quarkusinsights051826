package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Episode aggregate root — the source of truth for one show in the
 * QuarkusInsights program.
 *
 * <p>An Episode owns its state and protects its invariants. The
 * lifecycle is a state machine: {@link EpisodeStatus#SCHEDULED} →
 * {@link EpisodeStatus#LIVE} → {@link EpisodeStatus#PUBLISHED}, with a
 * branching transition to {@link EpisodeStatus#CANCELED} from
 * {@code SCHEDULED}. Each transition is gated by a behavior method
 * with explicit preconditions; status is never mutated directly.
 *
 * <p><strong>Aggregate boundary.</strong> The Abstract entity lives
 * inside this aggregate and is replaced by {@link #submitAbstract},
 * not edited in place. Cross-aggregate references (presenters,
 * speakers) are by {@link PersonId} only — never by direct Person
 * reference, in keeping with the design's prohibition on cross-context
 * pointer sharing.
 *
 * <p><strong>Domain events.</strong> Each behavior method that mutates
 * state appends a {@link DomainEvent} to the aggregate's internal
 * {@code recordedEvents} list. The application layer (Plan 2B) reads
 * the list via {@link #recordedEvents()} after persisting the
 * aggregate, then calls {@link #clearRecordedEvents()} to reset.
 *
 * <p><strong>Construction.</strong> The {@link #schedule} factory is
 * the only way to create a new Episode through normal flow.
 * {@link #rehydrate} exists solely for the persistence adapter to
 * reconstruct an Episode from stored state without recording an event;
 * application code MUST NOT call it.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class Episode {

    private final EpisodeId id;
    private final EpisodeNumber number;
    private final EpisodeTitle title;
    private final AirDate airDate;
    private Abstract theAbstract;
    private final Set<PersonId> presenters = new HashSet<>();
    private final Set<PersonId> speakers = new HashSet<>();
    private EpisodeStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> recordedEvents = new ArrayList<>();

    private Episode(EpisodeId id, EpisodeNumber number, EpisodeTitle title,
                    AirDate airDate, EpisodeStatus status, Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.airDate = airDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Schedules a new Episode and records an {@link EpisodeScheduled}
     * domain event.
     *
     * @param number  the sequential episode number
     * @param title   the display title
     * @param airDate the air date; must be today or later
     * @return a new Episode in {@link EpisodeStatus#SCHEDULED}
     * @throws AirDateInPast if {@code airDate} is strictly before today
     */
    public static Episode schedule(EpisodeNumber number, EpisodeTitle title, AirDate airDate) {
        if (airDate.value().isBefore(LocalDate.now())) {
            throw new AirDateInPast(airDate);
        }
        Instant now = Instant.now();
        Episode episode = new Episode(
            EpisodeId.random(), number, title, airDate,
            EpisodeStatus.SCHEDULED, now, now);
        episode.recordedEvents.add(new EpisodeScheduled(episode.id, number, airDate, now));
        return episode;
    }

    /**
     * Reconstructs an Episode from its persisted state.
     *
     * <p><strong>For persistence adapter use only.</strong> Application
     * code MUST NOT call this — use {@link #schedule} or load via the
     * {@link EpisodeRepository}. Rehydration does not record any
     * domain event because no domain action is occurring.
     *
     * @param id           the persisted id
     * @param number       the persisted number
     * @param title        the persisted title
     * @param airDate      the persisted air date
     * @param theAbstract  the persisted abstract, or null if none submitted
     * @param presenters   the persisted presenter PersonIds (copied defensively)
     * @param speakers     the persisted speaker PersonIds (copied defensively)
     * @param status       the persisted status
     * @param createdAt    the persisted creation timestamp
     * @param updatedAt    the persisted last-update timestamp
     * @return a hydrated Episode whose recordedEvents list is empty
     */
    public static Episode rehydrate(EpisodeId id, EpisodeNumber number, EpisodeTitle title,
                                    AirDate airDate, Abstract theAbstract,
                                    Set<PersonId> presenters, Set<PersonId> speakers,
                                    EpisodeStatus status, Instant createdAt, Instant updatedAt) {
        Episode episode = new Episode(id, number, title, airDate, status, createdAt, updatedAt);
        episode.theAbstract = theAbstract;
        episode.presenters.addAll(presenters);
        episode.speakers.addAll(speakers);
        return episode;
    }

    /** @return the aggregate's identifier */
    public EpisodeId id() { return id; }

    /** @return the episode's sequential number */
    public EpisodeNumber number() { return number; }

    /** @return the episode's display title */
    public EpisodeTitle title() { return title; }

    /** @return the episode's air date */
    public AirDate airDate() { return airDate; }

    /** @return the submitted abstract, or null if none has been submitted */
    public Abstract theAbstract() { return theAbstract; }

    /** @return an unmodifiable view of the presenter PersonIds */
    public Set<PersonId> presenters() { return Collections.unmodifiableSet(presenters); }

    /** @return an unmodifiable view of the speaker PersonIds */
    public Set<PersonId> speakers() { return Collections.unmodifiableSet(speakers); }

    /** @return the current lifecycle status */
    public EpisodeStatus status() { return status; }

    /** @return the instant the aggregate was created */
    public Instant createdAt() { return createdAt; }

    /** @return the instant of the last state-mutating behavior method */
    public Instant updatedAt() { return updatedAt; }

    /**
     * @return an unmodifiable snapshot of domain events recorded by
     *     this aggregate since the last {@link #clearRecordedEvents()}
     */
    public List<DomainEvent> recordedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recordedEvents));
    }

    /**
     * Clears the aggregate's recorded-events list. The application
     * layer calls this after publishing the events so subsequent
     * behavior calls do not redeliver the same events.
     */
    public void clearRecordedEvents() {
        recordedEvents.clear();
    }

    /**
     * Submits or replaces this episode's abstract.
     *
     * <p>If the episode already has an abstract, it is discarded and
     * replaced by a fresh {@link Abstract} with a new id. Allowed only
     * while the episode is in {@link EpisodeStatus#SCHEDULED}.
     *
     * <p>Records an {@link AbstractSubmitted} domain event.
     *
     * @param text the abstract text; must not be null
     * @throws IllegalArgumentException if {@code text} is null
     * @throws IllegalEpisodeTransition if the episode is not in
     *     {@link EpisodeStatus#SCHEDULED}
     */
    public void submitAbstract(AbstractText text) {
        if (text == null) {
            throw new IllegalArgumentException("AbstractText must not be null");
        }
        if (status != EpisodeStatus.SCHEDULED) {
            throw new IllegalEpisodeTransition(status);
        }
        Instant now = Instant.now();
        Abstract a = new Abstract(AbstractId.random(), text, now);
        this.theAbstract = a;
        this.updatedAt = now;
        recordedEvents.add(new AbstractSubmitted(id, a.id(), now));
    }

    /**
     * Assigns a {@link PersonId} as a presenter (host) on this episode.
     * Idempotent: assigning the same person twice is a no-op and does
     * not record a duplicate event. Allowed in
     * {@link EpisodeStatus#SCHEDULED} or {@link EpisodeStatus#LIVE}.
     *
     * <p>Records a {@link PresenterAssigned} event only if the person
     * was not already a presenter.
     *
     * @param personId the presenter's identifier; must not be null
     * @throws IllegalArgumentException if {@code personId} is null
     * @throws IllegalEpisodeTransition if the episode is not
     *     {@code SCHEDULED} or {@code LIVE}
     */
    public void assignPresenter(PersonId personId) {
        if (personId == null) {
            throw new IllegalArgumentException("PersonId must not be null");
        }
        if (status != EpisodeStatus.SCHEDULED && status != EpisodeStatus.LIVE) {
            throw new IllegalEpisodeTransition(status);
        }
        if (presenters.add(personId)) {
            Instant now = Instant.now();
            this.updatedAt = now;
            recordedEvents.add(new PresenterAssigned(id, personId, now));
        }
    }

    /**
     * Assigns a {@link PersonId} as a speaker (guest) on this episode.
     * Idempotent: assigning the same person twice is a no-op and does
     * not record a duplicate event. Allowed in
     * {@link EpisodeStatus#SCHEDULED} or {@link EpisodeStatus#LIVE}.
     *
     * <p>Records a {@link SpeakerAssigned} event only if the person
     * was not already a speaker.
     *
     * @param personId the speaker's identifier; must not be null
     * @throws IllegalArgumentException if {@code personId} is null
     * @throws IllegalEpisodeTransition if the episode is not
     *     {@code SCHEDULED} or {@code LIVE}
     */
    public void assignSpeaker(PersonId personId) {
        if (personId == null) {
            throw new IllegalArgumentException("PersonId must not be null");
        }
        if (status != EpisodeStatus.SCHEDULED && status != EpisodeStatus.LIVE) {
            throw new IllegalEpisodeTransition(status);
        }
        if (speakers.add(personId)) {
            Instant now = Instant.now();
            this.updatedAt = now;
            recordedEvents.add(new SpeakerAssigned(id, personId, now));
        }
    }

    /**
     * Transitions this episode from {@link EpisodeStatus#SCHEDULED} to
     * {@link EpisodeStatus#LIVE}.
     *
     * <p>Requires the air date to be on or before today's local date —
     * an episode cannot go live before its scheduled air date.
     *
     * <p>Records an {@link EpisodeWentLive} event.
     *
     * @throws IllegalEpisodeTransition if the episode is not
     *     {@code SCHEDULED}
     * @throws AirDateNotYetReached if the air date is strictly after today
     */
    public void goLive() {
        if (status != EpisodeStatus.SCHEDULED) {
            throw new IllegalEpisodeTransition(status);
        }
        if (airDate.value().isAfter(LocalDate.now())) {
            throw new AirDateNotYetReached(airDate);
        }
        Instant now = Instant.now();
        this.status = EpisodeStatus.LIVE;
        this.updatedAt = now;
        recordedEvents.add(new EpisodeWentLive(id, now));
    }

    /**
     * Transitions this episode from {@link EpisodeStatus#LIVE} to
     * {@link EpisodeStatus#PUBLISHED}.
     *
     * <p>Requires:
     * <ul>
     *   <li>current status is {@code LIVE} (otherwise
     *       {@link IllegalEpisodeTransition})</li>
     *   <li>an abstract has been submitted (otherwise
     *       {@link MissingAbstract})</li>
     *   <li>at least one presenter has been assigned (otherwise
     *       {@link MissingPresenter})</li>
     *   <li>at least one speaker has been assigned (otherwise
     *       {@link MissingSpeaker})</li>
     * </ul>
     *
     * <p>Records an {@link EpisodePublished} event.
     *
     * @throws IllegalEpisodeTransition if status is not {@code LIVE}
     * @throws MissingAbstract if no abstract has been submitted
     * @throws MissingPresenter if no presenters have been assigned
     * @throws MissingSpeaker if no speakers have been assigned
     */
    public void publish() {
        if (status != EpisodeStatus.LIVE) {
            throw new IllegalEpisodeTransition(status);
        }
        if (theAbstract == null) {
            throw new MissingAbstract(id);
        }
        if (presenters.isEmpty()) {
            throw new MissingPresenter(id);
        }
        if (speakers.isEmpty()) {
            throw new MissingSpeaker(id);
        }
        Instant now = Instant.now();
        this.status = EpisodeStatus.PUBLISHED;
        this.updatedAt = now;
        recordedEvents.add(new EpisodePublished(id, number, now));
    }

    /**
     * Cancels this episode while it is still in {@link EpisodeStatus#SCHEDULED}.
     * Cancellation is terminal — once canceled, the episode cannot be
     * reactivated (no transition out of {@code CANCELED} is provided).
     *
     * <p>Records an {@link EpisodeCanceled} event with the supplied reason.
     *
     * @param reason a human-readable cancellation reason; must not be
     *               null or blank
     * @throws IllegalArgumentException if {@code reason} is null or blank
     * @throws IllegalEpisodeTransition if status is not {@code SCHEDULED}
     */
    public void cancel(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancel reason must not be null or blank");
        }
        if (status != EpisodeStatus.SCHEDULED) {
            throw new IllegalEpisodeTransition(status);
        }
        Instant now = Instant.now();
        this.status = EpisodeStatus.CANCELED;
        this.updatedAt = now;
        recordedEvents.add(new EpisodeCanceled(id, reason, now));
    }
}
