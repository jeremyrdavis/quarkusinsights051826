# Step 7: Repository Port and Commands

The aggregate is complete. Now we start wiring it into an application. This step adds two things, both small and crucial: the **repository port** (an interface the domain defines, the infrastructure implements) and **command records** (the input shape for application-service methods). After this step, the domain layer can declare what it needs from persistence without depending on Panache, JDBC, or any database concept.

**Time:** ~15 min · **Files created:** 8 (`EpisodeRepository.java` + 7 commands) · **Tests added:** 0 (covered in Step 8)

## TL;DR

In `programming/domain/`, create the port:

```java
public interface EpisodeRepository {
    Optional<Episode> findById(EpisodeId id);
    Optional<Episode> findByNumber(EpisodeNumber number);
    List<Episode> findByStatus(EpisodeStatus status);
    void save(Episode episode);
}
```

In `programming/application/`, create seven command records — one per behavior method:

- `ScheduleEpisodeCommand(EpisodeNumber number, EpisodeTitle title, AirDate airDate)`
- `SubmitAbstractCommand(EpisodeId episodeId, AbstractText text)`
- `AssignPresenterCommand(EpisodeId episodeId, PersonId personId)`
- `AssignSpeakerCommand(EpisodeId episodeId, PersonId personId)`
- `GoLiveCommand(EpisodeId episodeId)`
- `PublishEpisodeCommand(EpisodeId episodeId)`
- `CancelEpisodeCommand(EpisodeId episodeId, String reason)`

Each command's compact constructor null-checks its arguments.

## Learning Objectives

By the end of this step you will be able to:

- Define a repository as an interface in the *domain* layer, not the infrastructure layer
- Explain why the dependency arrow points from infrastructure → domain, not the other way
- Use command records as the typed contract between REST and application service
- Justify why commands are records (immutable, equals-by-value) and use VOs as their fields

## Why This Matters

This step is the inflection point in hexagonal architecture: **inversion of dependencies**.

Naively, you'd expect `EpisodeService` to depend on `EpisodeRepositoryJpaImpl`, which in turn depends on a `DataSource`. Domain → application → infrastructure. That's the layered architecture you learned from textbooks, and it does keep things in roughly the right places, but it breaks down when the infrastructure layer's vocabulary leaks upward. Suddenly your domain method takes a `Pageable` because Spring Data does. Or a `Session` because Hibernate does. The lowest layer infects the highest.

Hexagonal architecture flips this. The domain declares ports — interfaces that describe what it *needs* from the outside world in its own vocabulary. The infrastructure layer *implements* those ports. The dependency arrow points **infrastructure → domain**, not the other way around.

```
                   ┌────────────────────┐
                   │      Domain        │ ◄── defines EpisodeRepository
                   │  (Episode, VOs,    │
                   │   events, port)    │
                   └────────────────────┘
                            ▲
                            │ implements
                   ┌────────────────────┐
                   │  Infrastructure    │
                   │  (Panache impl)    │
                   └────────────────────┘
```

This isn't aesthetic. It buys real properties:

- **The domain can be tested with no Quarkus boot.** We'll do this in Step 8 — swap in an `InMemoryEpisodeRepository` for unit tests, and the `EpisodeService` is none the wiser.
- **The persistence library can change without touching the domain.** Replace Panache with jOOQ, with MongoDB, with a remote API — only the implementation changes.
- **Mock generation is trivial.** A port is an interface; mocking frameworks love interfaces.

### Why commands?

A command is "what the user wants to happen, expressed in domain vocabulary." `ScheduleEpisodeCommand` is the typed shape of "I want to schedule episode #42 titled X to air on Y."

Couldn't the service just take three arguments? `service.scheduleEpisode(number, title, airDate)`? It could. But:

- **A command is one object that travels through one signature.** Refactoring is easier — adding a field is one place.
- **Commands name the use case.** `ScheduleEpisodeCommand` reads like business intent. Three arguments don't.
- **The REST layer can deserialize JSON directly into a command (almost) — it ends up being a thin DTO-to-command mapping.** That mapping is the place where transport-layer concerns (JSON, validation annotations) stop and domain-layer concerns (VOs, invariants) start.
- **Commands are records, which means equality and `toString` are free.** Useful in tests and logs.

Commands carry **value objects, not primitives**. Notice `ScheduleEpisodeCommand` has `EpisodeNumber number`, not `int number`. By the time a command exists, the VO has already validated the primitive — the rest of the application can trust the typed value. If the REST layer can't construct an `EpisodeNumber` from the request body (because it's `-3`), it fails at the boundary, not three layers deep.

## Implementation

Two folders to fill in.

### `programming/domain/EpisodeRepository.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.util.List;
import java.util.Optional;

public interface EpisodeRepository {

    Optional<Episode> findById(EpisodeId id);

    Optional<Episode> findByNumber(EpisodeNumber number);

    List<Episode> findByStatus(EpisodeStatus status);

    void save(Episode episode);
}
```

Four methods, chosen because the application service needs them:

- **`findById`** — to load an aggregate for a behavior call (the most common operation)
- **`findByNumber`** — for the uniqueness check in `schedule(...)`. Could be a separate `boolean numberExists(...)` method; we use `findByNumber` because it composes better (a caller wanting to know "what's currently scheduled as episode 42?" already has the answer).
- **`findByStatus`** — used by query endpoints later (list all `PUBLISHED` episodes, for example).
- **`save`** — handles both insert and update; the implementation chooses by id.

Three contract points the Javadoc on the solution emphasizes:

1. **`findById` and `findByNumber` return `Optional.empty()` on miss**, not `null` and not an exception. Missing aggregates are a legitimate query result; throwing forces every caller into a try/catch.
2. **`save` does not publish events.** The application service drains `recordedEvents()` after `save` returns. If the repository published, then a failure between save and publish would leave the database written but no event sent (or vice versa).
3. **No `delete` method.** Why? In this domain, episodes are never deleted — `CANCELED` is the closest you get. The repository's interface reflects what the *domain* allows, not what the database is capable of. If you don't put `delete` in the port, no application code can call it.

### `programming/application/` — Seven Commands

Create each file. They're all records. They all null-check.

```java
// ScheduleEpisodeCommand.java
public record ScheduleEpisodeCommand(EpisodeNumber number, EpisodeTitle title, AirDate airDate) {
    public ScheduleEpisodeCommand {
        if (number == null) throw new IllegalArgumentException("number must not be null");
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (airDate == null) throw new IllegalArgumentException("airDate must not be null");
    }
}
```

```java
// SubmitAbstractCommand.java
public record SubmitAbstractCommand(EpisodeId episodeId, AbstractText text) {
    public SubmitAbstractCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
        if (text == null) throw new IllegalArgumentException("text must not be null");
    }
}
```

```java
// AssignPresenterCommand.java
public record AssignPresenterCommand(EpisodeId episodeId, PersonId personId) {
    public AssignPresenterCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
        if (personId == null) throw new IllegalArgumentException("personId must not be null");
    }
}
```

```java
// AssignSpeakerCommand.java
public record AssignSpeakerCommand(EpisodeId episodeId, PersonId personId) {
    public AssignSpeakerCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
        if (personId == null) throw new IllegalArgumentException("personId must not be null");
    }
}
```

```java
// GoLiveCommand.java
public record GoLiveCommand(EpisodeId episodeId) {
    public GoLiveCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
    }
}
```

```java
// PublishEpisodeCommand.java
public record PublishEpisodeCommand(EpisodeId episodeId) {
    public PublishEpisodeCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
    }
}
```

```java
// CancelEpisodeCommand.java
public record CancelEpisodeCommand(EpisodeId episodeId, String reason) {
    public CancelEpisodeCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be null or blank");
        }
    }
}
```

Don't forget the `package` declarations and the `import` statements (the VO/ID types live in `programming/domain/` and `shared/` respectively).

### A note on duplication

Yes, four of these commands have only `episodeId` and one extra field. Yes, two of them (`GoLiveCommand`, `PublishEpisodeCommand`) are *identical* in shape. Could you use a single `EpisodeIdCommand`? Yes. **Don't.**

These records type the *intent*. `GoLiveCommand` and `PublishEpisodeCommand` may be structurally identical, but they're commands for different operations. A REST handler that accepts `EpisodeIdCommand` and dispatches based on URL is back to stringly-typed dispatch. Having one record per command keeps the type signature of the application service self-documenting: `void publish(PublishEpisodeCommand cmd)` says exactly what it does.

## Testing

These commands and the port have no behavior beyond null-checks; their semantics get exercised by the application service tests in Step 8. Skip testing them directly unless you want a sanity check that the null-checks fire — most teams don't bother.

## You should now see

After Step 7, `./mvnw test` reports the same count as Step 6 (around 81 tests). Eight new files exist:

- `programming/domain/EpisodeRepository.java`
- `programming/application/ScheduleEpisodeCommand.java`
- `programming/application/SubmitAbstractCommand.java`
- `programming/application/AssignPresenterCommand.java`
- `programming/application/AssignSpeakerCommand.java`
- `programming/application/GoLiveCommand.java`
- `programming/application/PublishEpisodeCommand.java`
- `programming/application/CancelEpisodeCommand.java`

Nothing implements `EpisodeRepository` yet — that's Step 9. The application service (Step 8) is going to depend only on the port.

**Next:** Step 8 — `EpisodeService`. We'll write the application service that takes commands, calls aggregate behaviors, persists the aggregate via the port, and publishes the recorded events. We'll test it without booting Quarkus, using an in-memory implementation of the port.
