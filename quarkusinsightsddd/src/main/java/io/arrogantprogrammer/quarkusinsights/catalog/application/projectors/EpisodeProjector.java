package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PresenterEmbeddable;
import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.SpeakerEmbeddable;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeSummary;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractSubmitted;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeCanceled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodePublished;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeScheduled;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeWentLive;
import io.arrogantprogrammer.quarkusinsights.programming.domain.PresenterAssigned;
import io.arrogantprogrammer.quarkusinsights.programming.domain.SpeakerAssigned;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Event projector that keeps the {@code public_episode_view} table up to date
 * with events from the Programming bounded context.
 *
 * <p>Part of the Public Catalog bounded context, application layer.
 *
 * <p>Subscribed events and their projector actions:
 * <ul>
 *   <li>{@link EpisodeScheduled} — creates a new {@link PublicEpisodeViewEntity}
 *       row with status SCHEDULED, empty presenters/speakers, zero comment/rating
 *       counts.</li>
 *   <li>{@link AbstractSubmitted} — queries {@link EpisodeQueries} for the
 *       real abstract text and stores it on the view row. Because the event fires
 *       within the same transaction as the Episode aggregate save, the text is
 *       guaranteed to be present at this point.</li>
 *   <li>{@link PresenterAssigned} — adds the presenter to the view's set; uses
 *       {@link PersonQueries} to resolve the display name. Idempotent: skips
 *       if the person is already in the set.</li>
 *   <li>{@link SpeakerAssigned} — same as PresenterAssigned but for the speakers
 *       set.</li>
 *   <li>{@link EpisodeWentLive} — updates status to LIVE.</li>
 *   <li>{@link EpisodePublished} — updates status to PUBLISHED.</li>
 *   <li>{@link EpisodeCanceled} — updates status to CANCELED. The row is kept
 *       so that users can still see a canceled episode in historical queries.</li>
 * </ul>
 *
 * <p>Each observer method is {@code @Transactional} to ensure the entity
 * mutation commits atomically. CDI fires the event synchronously within the
 * caller's transaction (or starts a new one if none exists), so the projector
 * write is part of the same unit of work as the originating command.
 */
@ApplicationScoped
public class EpisodeProjector {

    private final PersonQueries personQueries;
    private final EpisodeQueries episodeQueries;

    /**
     * Creates the projector.
     *
     * @param personQueries  cross-context query port for resolving person display names
     * @param episodeQueries cross-context query port for resolving episode titles and abstracts
     */
    @Inject
    public EpisodeProjector(PersonQueries personQueries, EpisodeQueries episodeQueries) {
        this.personQueries = personQueries;
        this.episodeQueries = episodeQueries;
    }

    /**
     * Handles {@link EpisodeScheduled}: inserts a new view row.
     *
     * <p>Resolves the real episode title via {@link EpisodeQueries}. Because
     * {@code EpisodeScheduled} is fired after the Episode aggregate is persisted
     * within the same transaction, the Episode row is guaranteed to be visible
     * to this observer. An {@link IllegalStateException} is thrown (rather than
     * silently falling back to a placeholder) so that any wiring bug surfaces
     * immediately during testing rather than shipping placeholder titles to users.
     *
     * @param event the scheduling event; never null (CDI guarantees this)
     * @throws IllegalStateException if no Episode with the given id exists in
     *     the database at the time this observer fires
     */
    @Transactional
    public void onEpisodeScheduled(@Observes EpisodeScheduled event) {
        EpisodeSummary summary = episodeQueries.findById(event.episodeId())
            .orElseThrow(() -> new IllegalStateException(
                "EpisodeScheduled event fired for unknown episode " + event.episodeId().value()
                + " — projector cannot enrich without the persisted Episode"));
        PublicEpisodeViewEntity entity = new PublicEpisodeViewEntity();
        entity.id = event.episodeId().value();
        entity.number = event.number().value();
        entity.title = summary.title().value();
        entity.airDate = event.airDate().value();
        entity.status = EpisodeStatus.SCHEDULED;
        entity.abstractText = null;
        entity.commentCount = 0;
        entity.ratingSum = null;
        entity.ratingCount = 0;
        entity.lastUpdatedAt = event.occurredAt();
        entity.persist();
    }

    /**
     * Handles {@link AbstractSubmitted}: stores the real abstract text on the view row.
     *
     * <p>Resolves the abstract text via {@link EpisodeQueries}. Because
     * {@code AbstractSubmitted} is fired after the Episode aggregate (including
     * the embedded Abstract) is persisted within the same transaction, the text
     * is guaranteed to be present at this point.
     *
     * @param event the abstract-submission event
     * @throws IllegalStateException if no Episode with the given id exists, or if
     *     the Episode has no abstract text at the time this observer fires
     */
    @Transactional
    public void onAbstractSubmitted(@Observes AbstractSubmitted event) {
        PublicEpisodeViewEntity entity = findByIdOrNull(event.episodeId().value());
        if (entity == null) return;
        EpisodeSummary summary = episodeQueries.findById(event.episodeId())
            .orElseThrow(() -> new IllegalStateException(
                "AbstractSubmitted event fired for unknown episode " + event.episodeId().value()));
        entity.abstractText = summary.abstractText()
            .map(AbstractText::value)
            .orElseThrow(() -> new IllegalStateException(
                "AbstractSubmitted event fired but Episode has no abstract — projector inconsistency"));
        entity.lastUpdatedAt = event.occurredAt();
    }

    /**
     * Handles {@link PresenterAssigned}: adds or updates the presenter entry.
     *
     * <p>Resolves the display name via {@link PersonQueries}. If the person is
     * not found (e.g., cross-context eventual consistency lag), the presenter
     * is added with a fallback display name of the personId UUID string.
     *
     * @param event the presenter-assignment event
     */
    @Transactional
    public void onPresenterAssigned(@Observes PresenterAssigned event) {
        PublicEpisodeViewEntity entity = findByIdOrNull(event.episodeId().value());
        if (entity == null) return;

        PersonId personId = event.personId();
        String displayName = resolveDisplayName(personId);

        // Idempotent: remove existing entry for this personId, then re-add
        entity.presenters.removeIf(p -> p.personId.equals(personId.value()));
        entity.presenters.add(new PresenterEmbeddable(personId.value(), displayName));
        entity.lastUpdatedAt = event.occurredAt();
    }

    /**
     * Handles {@link SpeakerAssigned}: adds or updates the speaker entry.
     *
     * @param event the speaker-assignment event
     */
    @Transactional
    public void onSpeakerAssigned(@Observes SpeakerAssigned event) {
        PublicEpisodeViewEntity entity = findByIdOrNull(event.episodeId().value());
        if (entity == null) return;

        PersonId personId = event.personId();
        String displayName = resolveDisplayName(personId);

        entity.speakers.removeIf(s -> s.personId.equals(personId.value()));
        entity.speakers.add(new SpeakerEmbeddable(personId.value(), displayName));
        entity.lastUpdatedAt = event.occurredAt();
    }

    /**
     * Handles {@link EpisodeWentLive}: transitions the view status to LIVE.
     *
     * @param event the go-live event
     */
    @Transactional
    public void onEpisodeWentLive(@Observes EpisodeWentLive event) {
        PublicEpisodeViewEntity entity = findByIdOrNull(event.episodeId().value());
        if (entity == null) return;
        entity.status = EpisodeStatus.LIVE;
        entity.lastUpdatedAt = event.occurredAt();
    }

    /**
     * Handles {@link EpisodePublished}: transitions the view status to PUBLISHED.
     *
     * @param event the publication event
     */
    @Transactional
    public void onEpisodePublished(@Observes EpisodePublished event) {
        PublicEpisodeViewEntity entity = findByIdOrNull(event.episodeId().value());
        if (entity == null) return;
        entity.status = EpisodeStatus.PUBLISHED;
        entity.lastUpdatedAt = event.occurredAt();
    }

    /**
     * Handles {@link EpisodeCanceled}: marks the view status as CANCELED.
     *
     * <p>The row is retained in the table — users may want to see cancelled
     * episodes in a historical context (e.g., "episode 7 was cancelled due to
     * technical difficulties"). The status change prevents the episode from
     * appearing in upcoming-show or published-history queries.
     *
     * @param event the cancellation event
     */
    @Transactional
    public void onEpisodeCanceled(@Observes EpisodeCanceled event) {
        PublicEpisodeViewEntity entity = findByIdOrNull(event.episodeId().value());
        if (entity == null) return;
        entity.status = EpisodeStatus.CANCELED;
        entity.lastUpdatedAt = event.occurredAt();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private PublicEpisodeViewEntity findByIdOrNull(java.util.UUID id) {
        return PublicEpisodeViewEntity.findById(id);
    }

    private String resolveDisplayName(PersonId personId) {
        return personQueries.findById(personId)
            .map(io.arrogantprogrammer.quarkusinsights.people.application.PersonSummary::displayName)
            .orElse(personId.value().toString());
    }
}
