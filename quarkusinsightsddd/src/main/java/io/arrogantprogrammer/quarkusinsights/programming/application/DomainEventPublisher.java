package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;

/**
 * Application-layer port for dispatching {@link DomainEvent}s
 * recorded by aggregates. Implementations are responsible for
 * delivering events to subscribers in this and other bounded
 * contexts.
 *
 * <p>The production implementation, {@link CdiDomainEventPublisher},
 * delegates to Quarkus / Jakarta EE's
 * {@link jakarta.enterprise.event.Event} mechanism, dispatching to
 * any {@code @Observes} method whose parameter type matches the
 * event's runtime type. Subscribers in other bounded contexts
 * (notably the Public Catalog projector) wire onto these events.
 *
 * <p>Tests substitute a recording stub
 * ({@code RecordingDomainEventPublisher}) so use case unit tests
 * remain pure JUnit with no Quarkus startup.
 *
 * <p>Part of the Programming bounded context, application layer.
 */
public interface DomainEventPublisher {

    /**
     * Publishes a single domain event. Implementations MUST deliver
     * the event to all matching subscribers before returning.
     *
     * @param event the event to publish; must not be null
     * @throws IllegalArgumentException if {@code event} is null
     */
    void publish(DomainEvent event);
}
