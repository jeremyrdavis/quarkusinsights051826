package io.arrogantprogrammer.quarkusinsights.engagement.application;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingRepository;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test-only implementation of {@link RatingRepository} backed by a
 * {@link HashMap}. Use case unit tests construct one of these,
 * pre-populate it with seed Ratings if needed, then pass it to the
 * service under test.
 *
 * <p>{@link #findByEpisodeAndAuthor} filters the in-memory map by both
 * episodeId and author, mirroring the UNIQUE constraint semantics that the
 * real implementation enforces at the database level (SPEC.md §3.6).
 */
public class InMemoryRatingRepository implements RatingRepository {

    private final Map<RatingId, Rating> store = new HashMap<>();

    @Override
    public Optional<Rating> findById(RatingId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Rating> findByEpisodeAndAuthor(EpisodeId episodeId, AuthorHandle author) {
        return store.values().stream()
            .filter(r -> r.episodeId().equals(episodeId) && r.author().equals(author))
            .findFirst();
    }

    @Override
    public List<Rating> findByEpisode(EpisodeId episodeId) {
        return store.values().stream()
            .filter(r -> r.episodeId().equals(episodeId))
            .toList();
    }

    @Override
    public void save(Rating rating) {
        store.put(rating.id(), rating);
    }

    /**
     * Test helper: returns the current count of stored Ratings.
     *
     * @return the count of stored Ratings
     */
    public int size() {
        return store.size();
    }
}
