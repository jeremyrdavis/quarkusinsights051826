package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AlreadyRated;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingRepository;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import io.arrogantprogrammer.quarkusinsights.shared.application.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Application service for the Rating aggregate.
 *
 * <p>Orchestrates the single Rating operation: submitting a star rating
 * from an audience member on a published episode. The method is a
 * transactional unit of work that enforces the uniqueness precheck,
 * creates the aggregate, persists it, and dispatches the recorded domain
 * events.
 *
 * <p><strong>§3.6 Dual-source {@link AlreadyRated} enforcement.</strong>
 * The "one Rating per (EpisodeId, AuthorHandle)" invariant is enforced in
 * this service by the {@code §3.6 PRECHECK} described below. An
 * {@link AlreadyRated} exception may be thrown from two distinct places
 * within a single call to {@link #submit}:
 *
 * <ol>
 *   <li><em>Precheck (this service)</em> — before creating the Rating
 *       aggregate, {@code ratingRepository.findByEpisodeAndAuthor} is
 *       called. If a matching Rating already exists, {@link AlreadyRated}
 *       is thrown immediately carrying the existing {@code RatingId}. This
 *       is the fast, deterministic path for serial duplicate submissions.</li>
 *   <li><em>Constraint-translation (repository adapter, Bundle 2)</em> —
 *       if two concurrent requests both pass the precheck and then race to
 *       insert, the database UNIQUE constraint on
 *       {@code (episode_id, author_handle)} will reject one. The
 *       {@code RatingRepositoryImpl} (added in Bundle 2) catches that
 *       persistence exception and re-throws {@link AlreadyRated}, so callers
 *       always see the same domain exception regardless of which enforcement
 *       path fired.</li>
 * </ol>
 *
 * <p>This is the demo's centerpiece teaching moment — the same exception
 * type surfaces from both the application-level precheck and the
 * infrastructure-level constraint translation. See SPEC.md §3.6.
 *
 * <p>Part of the Engagement bounded context, application layer.
 */
@ApplicationScoped
public class RatingService {

    private final RatingRepository ratingRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates the service.
     *
     * @param ratingRepository the Rating repository port
     * @param eventPublisher   the domain event publisher port
     */
    @Inject
    public RatingService(RatingRepository ratingRepository,
                         DomainEventPublisher eventPublisher) {
        this.ratingRepository = ratingRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Submits a new Rating and publishes a
     * {@link io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingSubmitted}
     * domain event.
     *
     * <p>Implements the §3.6 precheck: if the author has already rated
     * this episode, {@link AlreadyRated} is thrown before the Rating
     * aggregate is created.
     *
     * @param cmd the submit command; must not be null
     * @return the id of the freshly submitted Rating
     * @throws AlreadyRated if the author has already rated this episode
     *     (thrown from the precheck path) OR if a concurrent insert races
     *     past the precheck and hits the database constraint (thrown from
     *     the repository adapter's constraint-translation path in Bundle 2)
     */
    @Transactional
    public RatingId submit(SubmitRatingCommand cmd) {
        // §3.6 PRECHECK — first of three places where the rule is enforced
        ratingRepository.findByEpisodeAndAuthor(cmd.episodeId(), cmd.author())
            .ifPresent(existing -> {
                throw new AlreadyRated(existing.id(), cmd.episodeId(), cmd.author());
            });
        Rating rating = Rating.submit(cmd.episodeId(), cmd.author(), cmd.stars());
        ratingRepository.save(rating);  // can ALSO throw AlreadyRated if the
                                        // adapter catches a constraint violation
                                        // (see Bundle 2's RatingRepositoryImpl)
        dispatchEvents(rating);
        return rating.id();
    }

    private void dispatchEvents(Rating rating) {
        List<DomainEvent> events = rating.recordedEvents();
        rating.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
