package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingRepository;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;
import java.util.Optional;

/**
 * @QuarkusTest CDI bootstrap stub. Removed in Bundle 2 when the real
 * RatingRepositoryImpl is added.
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class StubRatingRepository implements RatingRepository {
    @Override public Optional<Rating> findById(RatingId id) { return Optional.empty(); }
    @Override public Optional<Rating> findByEpisodeAndAuthor(EpisodeId episodeId, AuthorHandle author) { return Optional.empty(); }
    @Override public List<Rating> findByEpisode(EpisodeId episodeId) { return List.of(); }
    @Override public void save(Rating rating) {}
}
