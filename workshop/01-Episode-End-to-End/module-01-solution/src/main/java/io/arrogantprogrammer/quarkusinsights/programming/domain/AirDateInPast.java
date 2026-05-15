package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Thrown by {@link Episode#schedule} when the supplied {@link AirDate}
 * is strictly before today's local date.
 *
 * <p>An episode cannot be scheduled in the past. Note that a current-day
 * AirDate is permitted (so an episode can be scheduled and immediately
 * brought live for same-day broadcasts).
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class AirDateInPast extends RuntimeException {

    private final AirDate airDate;

    /**
     * Creates an AirDateInPast exception.
     *
     * @param airDate the AirDate that was rejected
     */
    public AirDateInPast(AirDate airDate) {
        super("AirDate " + airDate.value() + " is in the past; episodes must be scheduled for today or later");
        this.airDate = airDate;
    }

    /**
     * @return the AirDate that triggered the exception
     */
    public AirDate airDate() {
        return airDate;
    }
}
