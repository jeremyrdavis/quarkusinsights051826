package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AlreadyRated;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingRepository;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of the {@link RatingRepository} port.
 *
 * <p>The "leak" of Panache active-record helpers is contained to this
 * adapter (per design doc §4.4): the entity-level {@code findById}/
 * {@code persist} calls live here only, not in any application or domain
 * code.
 *
 * <p><strong>§3.6 constraint translation — third enforcement place.</strong>
 * {@link #save} implements the third and final place where SPEC.md §3.6's
 * "one Rating per (EpisodeId, AuthorHandle)" invariant is enforced. When two
 * concurrent requests race past the {@code RatingService} precheck and both
 * attempt to insert, the database {@code uk_rating_episode_author} UNIQUE
 * constraint rejects one. This adapter catches the resulting
 * {@link PersistenceException}, unwraps the Hibernate
 * {@link ConstraintViolationException}, queries the existing Rating for its
 * id (for diagnostic clarity in the error response), and re-throws
 * {@link AlreadyRated} so callers see the same domain exception they would
 * have on the cooperative precheck path.
 *
 * <p><strong>Insert-only semantics for Rating.</strong> {@link Rating} is
 * immutable; there is no update path. {@code save} therefore always
 * {@code persist()}s a new entity. If the same Rating id is saved twice that
 * is a programming error — a duplicate-id {@code EntityExistsException} will
 * surface rather than being silently swallowed.
 *
 * <p>Part of the Engagement bounded context, infrastructure layer. See SPEC.md §3.6.
 */
@ApplicationScoped
public class RatingRepositoryImpl implements RatingRepository {

    private final RatingMapper mapper;

    /**
     * Creates the repository.
     *
     * @param mapper the entity ↔ aggregate translator
     */
    @Inject
    public RatingRepositoryImpl(RatingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Rating> findById(RatingId id) {
        RatingEntity entity = RatingEntity.findById(id.value());
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public Optional<Rating> findByEpisodeAndAuthor(EpisodeId episodeId, AuthorHandle author) {
        return RatingEntity.<RatingEntity>find(
                "episodeId = ?1 AND authorHandle = ?2",
                episodeId.value(), author.value())
            .firstResultOptional()
            .map(mapper::toDomain);
    }

    @Override
    public List<Rating> findByEpisode(EpisodeId episodeId) {
        return RatingEntity.<RatingEntity>find(
                "episodeId = ?1 ORDER BY submittedAt ASC", episodeId.value())
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * Persists a new Rating aggregate.
     *
     * <p>Rating is immutable; save always inserts a new entity. If a UNIQUE
     * constraint violation fires on {@code uk_rating_episode_author}, this
     * method translates the database exception to {@link AlreadyRated},
     * completing the §3.6 three-place rule.
     *
     * <p><strong>Constraint-translation strategy.</strong> PostgreSQL aborts the
     * transaction on a constraint violation, making it impossible to issue further
     * queries in the same transaction after the failure. To include the
     * pre-existing Rating's id in the {@link AlreadyRated} exception for "view
     * your previous rating" UX, this method queries {@code findByEpisodeAndAuthor}
     * <em>before</em> attempting the insert and caches the result. If the insert
     * then fails with the UNIQUE violation, the cached existing id is used without
     * any further database access.
     *
     * @param rating the aggregate to persist; must not be null
     * @throws AlreadyRated if the database UNIQUE constraint on
     *     {@code (episode_id, author_handle)} fires, indicating a concurrent
     *     race past the precheck
     */
    @Override
    public void save(Rating rating) {
        // Pre-query for an existing rating before the insert attempt.
        // This allows us to include the existing Rating's id in the AlreadyRated
        // exception without issuing a query after the transaction is aborted.
        // In the normal (non-duplicate) path this is one extra query but it is
        // only reached when the service-layer precheck was bypassed by a race.
        Rating existingBeforeInsert = findByEpisodeAndAuthor(rating.episodeId(), rating.author())
            .orElse(null);

        try {
            RatingEntity entity = mapper.toEntity(rating);
            entity.persist();
            RatingEntity.flush();
        } catch (PersistenceException e) {
            translateConstraintViolations(e, rating, existingBeforeInsert);
            throw e;
        }
    }

    private void translateConstraintViolations(PersistenceException e, Rating attempted,
                                               Rating existingBeforeInsert) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                if (name != null && name.toLowerCase().contains("uk_rating_episode_author")) {
                    // §3.6 BACKSTOP — third place where the rule is enforced.
                    // Use the pre-queried existing Rating (queried before the transaction was
                    // aborted by the constraint violation) to include its id in the exception.
                    if (existingBeforeInsert == null) {
                        // Extremely rare: another transaction inserted between our pre-query
                        // and this insert. We still throw AlreadyRated but cannot include the id.
                        throw new AlreadyRated(
                            io.arrogantprogrammer.quarkusinsights.shared.RatingId.random(),
                            attempted.episodeId(), attempted.author());
                    }
                    throw new AlreadyRated(existingBeforeInsert.id(), attempted.episodeId(), attempted.author());
                }
                return;
            }
            // Also check for the GenericJDBCException wrapping a PSQLException with
            // constraint violation message — Postgres aborts the transaction so Hibernate
            // may re-wrap as GenericJDBCException on subsequent statements.
            if (cause instanceof org.hibernate.exception.GenericJDBCException gjdbc) {
                String msg = gjdbc.getMessage();
                if (msg != null && msg.toLowerCase().contains("uk_rating_episode_author")) {
                    if (existingBeforeInsert != null) {
                        throw new AlreadyRated(existingBeforeInsert.id(), attempted.episodeId(), attempted.author());
                    }
                }
                // Check root PSQLException
                Throwable root = gjdbc.getCause();
                if (root != null) {
                    String rootMsg = root.getMessage();
                    if (rootMsg != null && rootMsg.toLowerCase().contains("uk_rating_episode_author")) {
                        if (existingBeforeInsert != null) {
                            throw new AlreadyRated(existingBeforeInsert.id(), attempted.episodeId(), attempted.author());
                        }
                    }
                }
            }
            cause = cause.getCause();
        }
    }
}
