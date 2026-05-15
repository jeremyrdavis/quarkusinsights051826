package io.arrogantprogrammer.quarkusinsights.programming.domain;

import java.time.LocalDate;

/**
 * Calendar date on which an episode airs (broadcast / streamed).
 *
 * <p>A Value Object — immutable, equals-by-value, wrapping a
 * {@link LocalDate}. AirDate carries no timezone or time-of-day; it
 * represents a date in the program's local calendar.
 *
 * <p>This VO does not impose temporal predicates such as "must be in
 * the future." Such checks belong to aggregate behavior — for example,
 * {@code Episode.schedule(...)} rejects an AirDate in the past, and
 * {@code Episode.goLive()} requires the AirDate to be on or before
 * today. Constructing an AirDate for any past date is permitted
 * because rehydrating an aggregate from persistence necessarily
 * requires reconstructing past AirDates.
 *
 * <p>Part of the Programming bounded context, domain layer.
 *
 * @param value the date; must not be null
 */
public record AirDate(LocalDate value) {

    /**
     * Creates an AirDate.
     *
     * @param value the wrapped date
     * @throws IllegalArgumentException if {@code value} is null
     */
    public AirDate {
        if (value == null) {
            throw new IllegalArgumentException("AirDate must not be null");
        }
    }
}
