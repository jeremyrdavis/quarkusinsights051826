package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentRepository;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test-only implementation of {@link CommentRepository} backed by a
 * {@link HashMap}. Use case unit tests construct one of these,
 * pre-populate it with seed Comments if needed, then pass it to the
 * service under test.
 */
public class InMemoryCommentRepository implements CommentRepository {

    private final Map<CommentId, Comment> store = new HashMap<>();

    @Override
    public Optional<Comment> findById(CommentId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Comment> findByEpisode(EpisodeId episodeId) {
        return store.values().stream()
            .filter(c -> c.episodeId().equals(episodeId))
            .sorted(Comparator.comparing(Comment::submittedAt))
            .toList();
    }

    @Override
    public void save(Comment comment) {
        store.put(comment.id(), comment);
    }

    /**
     * Test helper: returns the current count of stored Comments.
     *
     * @return the count of stored Comments
     */
    public int size() {
        return store.size();
    }
}
