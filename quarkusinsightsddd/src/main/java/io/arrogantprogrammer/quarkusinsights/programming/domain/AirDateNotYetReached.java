package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Thrown by {@link Episode#goLive()} when today's local date is strictly
 * before the episode's scheduled {@link AirDate}.
 *
 * <p>An episode cannot transition to {@code LIVE} before its scheduled
 * air date. Note that the air date is checked in the local calendar of
 * the program: a same-day broadcast is permitted (today equals airDate)
 * but a future air date is rejected.
 *
 * <p>This exception is distinct from {@link AirDateInPast}, which is
 * thrown by {@link Episode#schedule} when the air date is strictly
 * before today. Both map to HTTP 400 at the REST boundary but carry
 * different diagnostic meanings.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public class AirDateNotYetReached extends RuntimeException {

    private final AirDate airDate;

    /**
     * Creates an AirDateNotYetReached exception.
     *
     * @param airDate the AirDate that has not yet arrived
     */
    public AirDateNotYetReached(AirDate airDate) {
        super("AirDate " + airDate.value() + " has not yet arrived; the episode cannot go live until then");
        this.airDate = airDate;
    }

    /**
     * @return the AirDate that triggered the exception
     */
    public AirDate airDate() {
        return airDate;
    }
}
