package io.arrogantprogrammer.quarkusinsights.shared;

import java.time.Instant;

/**
 * Marker interface for all domain events. Concrete events are records inside
 * the producing context's `domain` package and carry only IDs and primitives —
 * they MUST NOT carry full aggregate references.
 */
public interface DomainEvent {

    /**
     * The instant at which this domain event was recorded by its aggregate.
     * Subscribers MAY use this to order or audit but MUST NOT use it to drive
     * business decisions (clock skew, replay, late arrivals make wall-time
     * unreliable for that purpose).
     */
    Instant occurredAt();
}
