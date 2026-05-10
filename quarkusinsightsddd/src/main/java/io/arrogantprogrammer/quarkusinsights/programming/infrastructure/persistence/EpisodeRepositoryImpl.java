package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumberAlreadyExists;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of the {@link EpisodeRepository}
 * port. The "leak" of Panache active-record helpers is contained to
 * this adapter (per design doc §4.4): the entity-level
 * {@code findById}/{@code persist} calls live here only, not in any
 * application or domain code.
 *
 * <p>{@link #save} additionally enforces the second half of the
 * cross-aggregate uniqueness rule from SPEC.md §3.6: when a
 * concurrent insertion races past the
 * {@code ScheduleEpisodeUseCase}'s pre-check, the database UNIQUE
 * constraint {@code uk_episode_number} fires and Hibernate raises
 * {@link ConstraintViolationException}; this adapter catches it and
 * re-throws {@link EpisodeNumberAlreadyExists} so callers see the
 * same domain exception they would have on the cooperative path.
 *
 * <p>Part of the Programming bounded context, infrastructure layer.
 */
@ApplicationScoped
public class EpisodeRepositoryImpl implements EpisodeRepository {

    private final EpisodeMapper mapper;

    /**
     * Creates the repository.
     *
     * @param mapper the entity ↔ aggregate translator
     */
    @Inject
    public EpisodeRepositoryImpl(EpisodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Episode> findById(EpisodeId id) {
        EpisodeEntity entity = EpisodeEntity.findById(id.value());
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public Optional<Episode> findByNumber(EpisodeNumber number) {
        return EpisodeEntity.<EpisodeEntity>find("number", number.value())
            .firstResultOptional()
            .map(mapper::toDomain);
    }

    @Override
    public List<Episode> findByStatus(EpisodeStatus status) {
        List<EpisodeEntity> entities = EpisodeEntity.list("status", status);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void save(Episode episode) {
        try {
            EpisodeEntity existing = EpisodeEntity.findById(episode.id().value());
            if (existing == null) {
                EpisodeEntity entity = mapper.toEntity(episode);
                entity.persist();
            } else {
                mapper.applyTo(episode, existing);
                // Panache flushes the managed entity on transaction commit.
            }
            EpisodeEntity.flush();
        } catch (PersistenceException e) {
            translateConstraintViolations(e, episode.number());
            throw e;
        }
    }

    private void translateConstraintViolations(PersistenceException e, EpisodeNumber attemptedNumber) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                // Match both the explicit constraint name and any auto-generated name
                // that Postgres/Hibernate creates for the 'number' column.
                if (name != null) {
                    String lower = name.toLowerCase();
                    if (lower.contains("uk_episode_number") || lower.contains("episode_number")) {
                        throw new EpisodeNumberAlreadyExists(attemptedNumber);
                    }
                }
                return;
            }
            cause = cause.getCause();
        }
    }
}
