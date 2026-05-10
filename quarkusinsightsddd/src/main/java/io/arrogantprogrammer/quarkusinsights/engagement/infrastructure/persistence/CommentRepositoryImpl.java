package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentRepository;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of the {@link CommentRepository} port.
 *
 * <p>The "leak" of Panache active-record helpers is contained to this
 * adapter (per design doc §4.4): the entity-level {@code findById}/
 * {@code persist} calls live here only, not in any application or domain
 * code.
 *
 * <p>{@link #save} implements insert-or-update semantics by checking for an
 * existing entity by id first. If found, {@link CommentMapper#applyTo} copies
 * the mutable body field onto the managed entity (Panache then flushes it on
 * transaction commit). Comment has no UNIQUE constraint beyond the primary key,
 * so no constraint-translation is needed here.
 *
 * <p>{@link #findByEpisode} returns Comments in chronological ascending order
 * by {@code submittedAt} so callers see the earliest comments first.
 *
 * <p>Part of the Engagement bounded context, infrastructure layer.
 */
@ApplicationScoped
public class CommentRepositoryImpl implements CommentRepository {

    private final CommentMapper mapper;

    /**
     * Creates the repository.
     *
     * @param mapper the entity ↔ aggregate translator
     */
    @Inject
    public CommentRepositoryImpl(CommentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Comment> findById(CommentId id) {
        CommentEntity entity = CommentEntity.findById(id.value());
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public List<Comment> findByEpisode(EpisodeId episodeId) {
        return CommentEntity.<CommentEntity>find(
                "episodeId = ?1 ORDER BY submittedAt ASC", episodeId.value())
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void save(Comment comment) {
        CommentEntity existing = CommentEntity.findById(comment.id().value());
        if (existing == null) {
            CommentEntity entity = mapper.toEntity(comment);
            entity.persist();
        } else {
            mapper.applyTo(comment, existing);
            // Panache flushes the managed entity on transaction commit.
        }
        CommentEntity.flush();
    }
}
