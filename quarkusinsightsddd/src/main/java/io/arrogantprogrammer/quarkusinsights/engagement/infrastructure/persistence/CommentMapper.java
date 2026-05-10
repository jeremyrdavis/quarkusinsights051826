package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Translates between the {@link Comment} domain aggregate and the
 * {@link CommentEntity} JPA persistence model.
 *
 * <p>The mapper is the single bridge between the two worlds: the
 * domain knows nothing about JPA, and the entity knows nothing about
 * domain methods. Construction direction:
 * <ul>
 *   <li>{@link #toEntity}: produces a fresh {@code CommentEntity}
 *       from a Comment, suitable for {@code persist()}.</li>
 *   <li>{@link #toDomain}: produces a fresh {@link Comment} from a
 *       loaded entity using {@link Comment#rehydrate} (does not record
 *       any domain events).</li>
 *   <li>{@link #applyTo}: copies an Comment's mutable state onto an
 *       already-managed entity. Only {@code body} is mutable (the
 *       5-minute edit window permits one body update).</li>
 * </ul>
 *
 * <p>Part of the Engagement bounded context, infrastructure layer.
 */
@ApplicationScoped
public class CommentMapper {

    /**
     * Builds a fresh {@link CommentEntity} mirroring the given {@link Comment}.
     * Used when persisting a new aggregate.
     *
     * @param comment the source aggregate; must not be null
     * @return a fresh entity with all fields populated
     */
    public CommentEntity toEntity(Comment comment) {
        CommentEntity entity = new CommentEntity();
        entity.id = comment.id().value();
        entity.episodeId = comment.episodeId().value();
        entity.authorHandle = comment.author().value();
        entity.body = comment.body().value();
        entity.submittedAt = comment.submittedAt();
        return entity;
    }

    /**
     * Reconstructs a {@link Comment} aggregate from its persisted state.
     * Uses {@link Comment#rehydrate} so no domain events are recorded —
     * load is not a domain action.
     *
     * @param entity the loaded entity; must not be null
     * @return the rehydrated Comment
     */
    public Comment toDomain(CommentEntity entity) {
        return Comment.rehydrate(
            new CommentId(entity.id),
            new EpisodeId(entity.episodeId),
            new AuthorHandle(entity.authorHandle),
            new CommentBody(entity.body),
            entity.submittedAt
        );
    }

    /**
     * Copies the mutable state of a {@link Comment} onto an
     * already-managed {@link CommentEntity}, preserving the entity's
     * identity, submission timestamp, and {@code @Version}.
     *
     * <p>Only {@code body} can change on a Comment (via the 5-minute edit
     * window). All other fields are final on the aggregate and must not
     * be copied here.
     *
     * @param comment the source aggregate (post-edit)
     * @param entity  the loaded entity to update in place
     */
    public void applyTo(Comment comment, CommentEntity entity) {
        entity.body = comment.body().value();
    }
}
