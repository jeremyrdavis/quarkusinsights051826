package io.arrogantprogrammer.quarkusinsights.shared.application;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only implementation of {@link DomainEventPublisher} that
 * records every published event in order. Use case unit tests
 * inspect {@link #publishedEvents()} after calling the use case to
 * verify the right events fired.
 *
 * <p>Part of the shared kernel's application support — shared by all
 * bounded-context unit test suites.
 */
public class RecordingDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        events.add(event);
    }

    /** @return an unmodifiable snapshot of events published so far */
    public List<DomainEvent> publishedEvents() {
        return List.copyOf(events);
    }
}
