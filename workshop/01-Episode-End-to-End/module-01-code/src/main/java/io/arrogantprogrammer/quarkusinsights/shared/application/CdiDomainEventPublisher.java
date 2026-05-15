package io.arrogantprogrammer.quarkusinsights.shared.application;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * Production implementation of {@link DomainEventPublisher} that
 * dispatches events via Jakarta EE / Quarkus CDI's
 * {@link Event#fire} mechanism.
 *
 * <p>Part of the shared kernel's application support — used by all
 * bounded contexts that publish domain events.
 */
@ApplicationScoped
public class CdiDomainEventPublisher implements DomainEventPublisher {

    private final Event<DomainEvent> events;

    /**
     * Creates a CdiDomainEventPublisher.
     *
     * @param events the CDI-managed event dispatcher; injected by
     *               the container
     */
    @Inject
    public CdiDomainEventPublisher(Event<DomainEvent> events) {
        this.events = events;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        events.fire(event);
    }
}
