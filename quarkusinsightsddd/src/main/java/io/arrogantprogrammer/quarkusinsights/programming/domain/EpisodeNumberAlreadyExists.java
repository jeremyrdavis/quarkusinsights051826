package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Thrown when {@code ScheduleEpisodeUseCase} attempts to schedule an
 * Episode whose number is already in use.
 *
 * <p>Episode number uniqueness is a cross-aggregate invariant — no
 * single Episode aggregate can verify it, so the rule is enforced at
 * the application service layer (and backed by a database UNIQUE
 * constraint at the persistence layer in Plan 2C). See SPEC.md §3.6
 * for the canonical "rule in three places" pattern.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class EpisodeNumberAlreadyExists extends RuntimeException {

    private final EpisodeNumber number;

    /**
     * Creates an EpisodeNumberAlreadyExists exception.
     *
     * @param number the episode number that was already taken
     */
    public EpisodeNumberAlreadyExists(EpisodeNumber number) {
        super("Episode number " + number.value() + " is already in use");
        this.number = number;
    }

    /**
     * @return the episode number that was already taken
     */
    public EpisodeNumber number() {
        return number;
    }
}
