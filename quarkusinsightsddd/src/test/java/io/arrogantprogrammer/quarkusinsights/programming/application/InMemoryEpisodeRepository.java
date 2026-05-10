package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test-only implementation of {@link EpisodeRepository} backed by a
 * {@link HashMap}. Use case unit tests construct one of these,
 * pre-populate it with seed Episodes if needed, then pass it to the
 * use case under test.
 */
public class InMemoryEpisodeRepository implements EpisodeRepository {

    private final Map<EpisodeId, Episode> store = new HashMap<>();

    @Override
    public Optional<Episode> findById(EpisodeId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Episode> findByNumber(EpisodeNumber number) {
        return store.values().stream()
            .filter(e -> e.number().equals(number))
            .findFirst();
    }

    @Override
    public List<Episode> findByStatus(EpisodeStatus status) {
        return store.values().stream()
            .filter(e -> e.status() == status)
            .toList();
    }

    @Override
    public void save(Episode episode) {
        store.put(episode.id(), episode);
    }

    /**
     * Test helper: returns the current count of stored Episodes.
     *
     * @return the count of stored Episodes
     */
    public int size() {
        return store.size();
    }
}
