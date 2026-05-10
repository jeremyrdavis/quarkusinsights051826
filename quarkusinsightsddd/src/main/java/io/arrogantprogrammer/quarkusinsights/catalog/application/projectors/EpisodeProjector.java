package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PresenterEmbeddable;
import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.SpeakerEmbeddable;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractSubmitted;
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

import java.time.Instant;

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
 *   <li>{@link AbstractSubmitted} — marks the entity to record that an abstract
 *       was submitted. <strong>Known limitation:</strong> the current event does
 *       not include the abstract text, only the AbstractId. The projection records
 *       a placeholder to indicate an abstract was submitted; the actual text
 *       requires a cross-context query or an enriched event payload (deferred to
 *       a future plan).</li>
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

    /**
     * Creates the projector.
     *
     * @param personQueries cross-context query port for resolving person display names
     */
    @Inject
    public EpisodeProjector(PersonQueries personQueries) {
        this.personQueries = personQueries;
    }

    /**
     * Handles {@link EpisodeScheduled}: inserts a new view row.
     *
     * @param event the scheduling event; never null (CDI guarantees this)
     */
    @Transactional
    public void onEpisodeScheduled(@Observes EpisodeScheduled event) {
        PublicEpisodeViewEntity entity = new PublicEpisodeViewEntity();
        entity.id = event.episodeId().value();
        entity.number = event.number().value();
        entity.title = "Episode " + event.number().value(); // placeholder; title not in event
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
     * Handles {@link AbstractSubmitted}: records that an abstract was submitted.
     *
     * <p><strong>Known limitation:</strong> {@link AbstractSubmitted} does not
     * carry the abstract text, only the {@code AbstractId}. The projector
     * therefore cannot populate the actual text at this time. A real
     * implementation would either:
     * <ol>
     *   <li>Extend {@code AbstractSubmitted} to include the text, or</li>
     *   <li>Perform a cross-context query to the Programming repository.</li>
     * </ol>
     * For now, the projector sets a sentinel value to indicate the abstract
     * exists, deferring text enrichment to a future plan.
     *
     * @param event the abstract-submission event
     */
    @Transactional
    public void onAbstractSubmitted(@Observes AbstractSubmitted event) {
        PublicEpisodeViewEntity entity = findByIdOrNull(event.episodeId().value());
        if (entity == null) return;
        // Abstract text not available in the event payload — see Javadoc above.
        entity.abstractText = "[abstract submitted — text pending enrichment]";
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
