package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentRepository;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;
import java.util.Optional;

/**
 * @QuarkusTest CDI bootstrap stub. Removed in Bundle 2 when the real
 * CommentRepositoryImpl is added.
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class StubCommentRepository implements CommentRepository {
    @Override public Optional<Comment> findById(CommentId id) { return Optional.empty(); }
    @Override public List<Comment> findByEpisode(EpisodeId id) { return List.of(); }
    @Override public void save(Comment c) {}
}
