# Step 8: Application Service

We have an aggregate that knows business rules and a port that says how to persist it. Now we connect them with the application service — the thin orchestrator that loads, calls, saves, and publishes. By the end of this step, all seven Episode operations are callable end-to-end against an in-memory repository, with no Quarkus boot and no real database.

**Time:** ~30 min · **Files created:** 2 (`EpisodeService.java` + test scaffolding) · **Tests added:** ~27

## TL;DR

Create `EpisodeService.java` in `programming/application/`. CDI-managed (`@ApplicationScoped`), constructor-injects the repository port and a `DomainEventPublisher` port. Seven `@Transactional` methods — one per command. Each follows the same five-line shape:

```java
@Transactional
public void submitAbstract(SubmitAbstractCommand cmd) {
    Episode episode = loadOrThrow(cmd.episodeId());
    episode.submitAbstract(cmd.text());
    episodeRepository.save(episode);
    dispatchEvents(episode);
}
```

…plus `schedule(...)` which is slightly different (no load — it's a factory operation).

## Learning Objectives

By the end of this step you will be able to:

- Explain what an application service *is* and *is not* responsible for
- Write the load → call → save → publish loop and explain each step
- Implement a cross-aggregate invariant check (episode-number uniqueness) at the service layer
- Test the service with an in-memory repository, no Quarkus boot

## Why This Matters

The application service is the thinnest layer in DDD, and it's the one most likely to be overstuffed. The temptation is real: it's the natural place to put validation, business rules, response shaping, logging, authorization. **Resist all of these.** The application service should be **boring**.

Boring how? Like this:

1. **Load** the aggregate (or, for factory ops, prepare to create it)
2. **Call** the appropriate behavior method on the aggregate
3. **Save** the aggregate through the repository port
4. **Publish** any recorded events
5. **Return** (usually `void` or just the new aggregate's ID)

That's the whole template. Every method in `EpisodeService` is one of those five lines variations on those five steps.

What goes into the aggregate? **Business rules.** What goes into the REST adapter? **Transport rules** (JSON, HTTP codes). What's left in between? **Coordination.** That's the service's job: load this, call that, save, publish. Adding business logic here is the slippery slope back to anemic CRUD.

### The one exception: cross-aggregate invariants

Cross-aggregate invariants — rules that span multiple aggregates — can't be enforced by any single aggregate. The only one in our domain is episode-number uniqueness: "no two Episodes can have the same number." `Episode.schedule(...)` cannot check this; it only knows about itself.

So the check has to live somewhere. The application service is the right place:

```java
episodeRepository.findByNumber(cmd.number()).ifPresent(existing -> {
    throw new EpisodeNumberAlreadyExists(cmd.number());
});
```

This is the **first of the three places** the design spec calls "the three-place rule for uniqueness." The other two are the database `UNIQUE` constraint (Step 9) and the adapter-level exception translator that turns a DB constraint violation into the same `EpisodeNumberAlreadyExists`. Why all three? Because each catches the rule at a different layer:

- The service check is fast and gives a clean error before any DB write.
- The DB constraint catches races (two services concurrently checking, both finding empty, both inserting).
- The translator ensures the error type is consistent whether the failure came from the service check or the DB.

For now (Step 8), we'll do the service check. The other two land in Step 9.

### Transactions

Every method is `@Transactional`. The boundary is "one command = one transaction." That matters because:

- The aggregate's save and the events are durable together. If the publish path fails after the save, the transaction rolls back the save too (assuming the publisher implementation supports tx — CDI's default does).
- A single aggregate is the unit of consistency. Spanning multiple aggregates in one transaction is a smell — it means you've drawn an aggregate boundary wrong.

### The dependency on the *port*, not the implementation

```java
@Inject
public EpisodeService(EpisodeRepository episodeRepository,
                      DomainEventPublisher eventPublisher) { … }
```

Both are interfaces (one defined in `programming/domain/`, the other in `shared/application/`). The implementations are in `programming/infrastructure/persistence/` and `shared/application/CdiDomainEventPublisher` — neither is mentioned here. The service doesn't know what's behind the port.

In production, CDI injects `EpisodeRepositoryImpl` and `CdiDomainEventPublisher`. In tests (below), we'll inject `InMemoryEpisodeRepository` and `RecordingDomainEventPublisher`. Same service, different wiring. **This is the payoff of Step 7.**

## Implementation

### `EpisodeService.java`

Put this in `src/main/java/io/arrogantprogrammer/quarkusinsights/programming/application/`.

```java
package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumberAlreadyExists;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.application.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final DomainEventPublisher eventPublisher;

    @Inject
    public EpisodeService(EpisodeRepository episodeRepository,
                          DomainEventPublisher eventPublisher) {
        this.episodeRepository = episodeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EpisodeId schedule(ScheduleEpisodeCommand cmd) {
        episodeRepository.findByNumber(cmd.number()).ifPresent(existing -> {
            throw new EpisodeNumberAlreadyExists(cmd.number());
        });
        Episode episode = Episode.schedule(cmd.number(), cmd.title(), cmd.airDate());
        episodeRepository.save(episode);
        dispatchEvents(episode);
        return episode.id();
    }

    @Transactional
    public void submitAbstract(SubmitAbstractCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.submitAbstract(cmd.text());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    @Transactional
    public void assignPresenter(AssignPresenterCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.assignPresenter(cmd.personId());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    @Transactional
    public void assignSpeaker(AssignSpeakerCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.assignSpeaker(cmd.personId());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    @Transactional
    public void goLive(GoLiveCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.goLive();
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    @Transactional
    public void publish(PublishEpisodeCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.publish();
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    @Transactional
    public void cancel(CancelEpisodeCommand cmd) {
        Episode episode = loadOrThrow(cmd.episodeId());
        episode.cancel(cmd.reason());
        episodeRepository.save(episode);
        dispatchEvents(episode);
    }

    private Episode loadOrThrow(EpisodeId id) {
        return episodeRepository.findById(id)
            .orElseThrow(() -> new EpisodeNotFound(id));
    }

    private void dispatchEvents(Episode episode) {
        List<DomainEvent> events = episode.recordedEvents();
        episode.clearRecordedEvents();
        events.forEach(eventPublisher::publish);
    }
}
```

A few details to note:

- **`schedule` is the odd one out.** It's a factory operation, not a load-then-call. It does the uniqueness check first, then creates the aggregate, saves, dispatches, returns the new ID. The other six methods return `void` because they mutate an existing aggregate.
- **`loadOrThrow` is a private helper.** Every load uses it; the consistent translation of empty-Optional → `EpisodeNotFound` lives in one place.
- **`dispatchEvents` is also a helper.** Read events, clear the aggregate's buffer, then fire. **The clear happens before the fire** — if a subscriber were to call back into the service and trigger another save of the same aggregate (don't do this, but…), we'd avoid double-dispatching the same events.
- **The publisher is the port, not the CDI `Event`.** The service depends on `DomainEventPublisher`; the production wiring uses `CdiDomainEventPublisher` (already in `shared/application/`). The test will swap in a `RecordingDomainEventPublisher`.

That's the whole production code for this step. Now the testing.

## Testing

Application-service tests are the prime example of **fast unit tests that exercise real domain logic without booting Quarkus.** Two test-only pieces help:

### Test scaffolding 1: `InMemoryEpisodeRepository`

Put this in `src/test/java/io/arrogantprogrammer/quarkusinsights/programming/application/`.

```java
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

public class InMemoryEpisodeRepository implements EpisodeRepository {

    private final Map<EpisodeId, Episode> store = new HashMap<>();

    @Override public Optional<Episode> findById(EpisodeId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override public Optional<Episode> findByNumber(EpisodeNumber number) {
        return store.values().stream().filter(e -> e.number().equals(number)).findFirst();
    }

    @Override public List<Episode> findByStatus(EpisodeStatus status) {
        return store.values().stream().filter(e -> e.status() == status).toList();
    }

    @Override public void save(Episode episode) {
        store.put(episode.id(), episode);
    }

    public int size() { return store.size(); }
}
```

A `HashMap` plus a stream-filter or two. That's the whole "fake database" needed for unit tests. No mocking framework involved.

### Test scaffolding 2: `RecordingDomainEventPublisher` (already exists)

This one already lives in `src/test/java/io/arrogantprogrammer/quarkusinsights/shared/application/` — it was part of the starter project's shared kernel. Verify it's there:

```bash
ls src/test/java/io/arrogantprogrammer/quarkusinsights/shared/application/
# Should show: RecordingDomainEventPublisher.java
```

It's an implementation of `DomainEventPublisher` that just appends events to a list, so tests can read back what was published.

### `EpisodeServiceTest.java`

The shape: one outer test class, seven `@Nested` inner classes (one per method).

```java
package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.*;
import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.arrogantprogrammer.quarkusinsights.shared.application.RecordingDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EpisodeServiceTest {

    private InMemoryEpisodeRepository repo;
    private RecordingDomainEventPublisher publisher;
    private EpisodeService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryEpisodeRepository();
        publisher = new RecordingDomainEventPublisher();
        service = new EpisodeService(repo, publisher);
    }

    @Nested
    class Schedule {

        @Test
        void persistsAndReturnsId() {
            EpisodeId id = service.schedule(new ScheduleEpisodeCommand(
                new EpisodeNumber(1),
                new EpisodeTitle("Pilot"),
                new AirDate(LocalDate.now().plusDays(1))
            ));
            assertNotNull(id);
            assertEquals(1, repo.size());
            assertTrue(repo.findById(id).isPresent());
        }

        @Test
        void publishesEpisodeScheduledEvent() {
            service.schedule(new ScheduleEpisodeCommand(
                new EpisodeNumber(1), new EpisodeTitle("Pilot"),
                new AirDate(LocalDate.now().plusDays(1))));
            List<DomainEvent> events = publisher.publishedEvents();
            assertEquals(1, events.size());
            assertInstanceOf(EpisodeScheduled.class, events.get(0));
        }

        @Test
        void rejectsDuplicateNumber() {
            service.schedule(new ScheduleEpisodeCommand(
                new EpisodeNumber(42), new EpisodeTitle("First"),
                new AirDate(LocalDate.now().plusDays(1))));
            assertThrows(EpisodeNumberAlreadyExists.class, () ->
                service.schedule(new ScheduleEpisodeCommand(
                    new EpisodeNumber(42), new EpisodeTitle("Duplicate"),
                    new AirDate(LocalDate.now().plusDays(2)))));
        }

        // ... and so on for the other use cases
    }

    @Nested
    class SubmitAbstract { /* 4 tests */ }

    @Nested
    class AssignPresenter { /* 4 tests */ }

    @Nested
    class AssignSpeaker { /* 4 tests */ }

    @Nested
    class GoLive { /* 3 tests */ }

    @Nested
    class Publish { /* 3 tests */ }

    @Nested
    class Cancel { /* 3 tests */ }
}
```

The full version is in `module-01-solution/src/test/java/.../EpisodeServiceTest.java` — refer to it for the exact assertions in the other `@Nested` blocks.

Each block tests three things (give or take):

1. **The happy path** — the aggregate ends up in the right state, the repository has the right contents.
2. **The right event was published** — `RecordingDomainEventPublisher` lets us assert this directly.
3. **The error cases** — `EpisodeNotFound` when ID doesn't exist, `IllegalEpisodeTransition` when status is wrong, etc.

Notice **no `@QuarkusTest` annotation.** These are plain JUnit tests. They run in milliseconds, not seconds. The whole `EpisodeServiceTest` class — 27 tests — runs in under 100ms total.

Run it:

```bash
./mvnw test -Dtest='EpisodeServiceTest'
```

## You should now see

After Step 8, `./mvnw test` reports approximately:

```
[INFO] Tests run: 108, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

(81 from earlier + ~27 application service tests.)

You have a complete application service that can execute all seven Episode operations end-to-end against an in-memory store. Everything from REST request → aggregate behavior → event publishing works — except there's no REST and no real persistence. That's the next two steps.

**Next:** Step 9 — the persistence adapter. We implement `EpisodeRepository` with Panache + PostgreSQL, and learn why mapping between domain and persistence models matters.
