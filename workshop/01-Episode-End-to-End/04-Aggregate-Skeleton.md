# Step 4: Aggregate Skeleton

This is the keystone step. So far we've built parts (values, events, exceptions) — nothing has used any of them. Now we build the `Episode` aggregate, the thing that uses all of them. By the end of this step you'll have a `schedule(...)` factory that creates a fresh episode, a `rehydrate(...)` factory that loads one from storage, accessors for every field, and the recorded-events machinery that lets the application layer publish events later.

We're **not** doing the behavior methods (`submitAbstract`, `goLive`, etc.) yet — those are Steps 5 and 6. This step is the *shape*.

**Time:** ~30 min · **Files created:** 2 (`Abstract.java` + `Episode.java`) · **Tests added:** 10

## TL;DR

Create `Abstract.java` (a record entity) and `Episode.java` (the aggregate root). Episode has:

- private constructor (no public `new Episode(...)`)
- `static schedule(EpisodeNumber, EpisodeTitle, AirDate)` factory that records an `EpisodeScheduled` event
- `static rehydrate(...)` factory for the persistence adapter (records *no* event)
- Accessors for every field
- `recordedEvents()` / `clearRecordedEvents()` for the application layer to drain events after publish

## Learning Objectives

By the end of this step you will be able to:

- Define the *aggregate boundary* — what's inside, what's outside, what crosses the line as IDs
- Justify why the constructor is private and creation goes through static factories
- Explain why `schedule(...)` and `rehydrate(...)` are different methods, not one constructor with a flag
- Explain how recorded events sit on the aggregate until the application layer drains them

## Why This Matters

The aggregate is *the* DDD pattern. Without it, you have value objects and events floating around an anemic data model — you've moved code but not solved the problem. With it, you have a place where every change to episode state passes through behavior that enforces invariants and emits events.

### What is an aggregate?

An aggregate is a **cluster of related objects treated as a single unit for consistency**. The boundary is real:

- **Inside the boundary**: the aggregate root (`Episode`) and any inside entities (`Abstract`). State changes here are atomic with respect to each other.
- **Outside the boundary**: other aggregates (people, comments, ratings). These are referenced **only by ID**, not by direct pointer.

The rule that follows is iron: **other code may load an Episode only through `EpisodeRepository`, may mutate it only through methods on `Episode`, and may persist it only through `EpisodeRepository`.** No back doors. No "let me reach in and update the status field." The aggregate either accepts a change (and records the event) or refuses it (with a typed exception).

This is what makes the architecture *agent-friendly* in the way the talk's abstract describes: an LLM (or a careless human) generating glue code cannot accidentally bypass a domain rule, because the rule is enforced by *structure*, not convention. There is no setter to call.

### Why IDs across the boundary, not pointers

`Episode` has `Set<PersonId> presenters` and `Set<PersonId> speakers` — not `Set<Person> presenters`. That's not laziness; it's the rule. **Direct references between aggregates break consistency boundaries.** If `Episode` held a `Person` reference, a mutation to that Person via *another* code path would silently change what `Episode` sees, and the aggregate's invariants would no longer be enforceable. ID-only references keep aggregates independent.

When an application service needs both an Episode and a Person, it loads each through its own repository. Slower? Marginally. Safer? Decisively.

### Why a private constructor

```java
private Episode(EpisodeId id, EpisodeNumber number, …) { … }

public static Episode schedule(EpisodeNumber number, EpisodeTitle title, AirDate airDate) { … }
public static Episode rehydrate(EpisodeId id, …) { … }
```

If `new Episode(...)` were public, any code could construct an Episode bypassing the rules in `schedule(...)` (e.g., `AirDateInPast`). Factory methods are gates: every Episode that comes into existence passes through one. New episodes? `schedule(...)`. Loaded from DB? `rehydrate(...)`. No third option.

### Why `schedule` and `rehydrate` are different

`schedule(...)` is a **domain action**: it represents a business event (a new episode planned) and records an `EpisodeScheduled` event. `rehydrate(...)` is **infrastructure**: it reconstructs an aggregate from previously-recorded state, and records *no* event because no business action is occurring. Combining them ("constructor that records events unless you pass `loading=true`") would mean every aggregate creation has to think about both modes, and a bug at the seam would falsely emit a `EpisodeScheduled` event for every episode you load from the database.

Keep them separate. The persistence layer (Step 9) will call `rehydrate`. Nobody else.

### The recorded-events buffer

The aggregate records events as it goes, into an internal `List<DomainEvent>`. It does **not** publish them. Publishing requires:

- A transaction (event subscribers may write to other tables)
- Knowledge of the publisher (CDI? Kafka? SNS?)

The aggregate doesn't know about any of that. So it stores events; the application service (Step 8) reads `recordedEvents()` after `save(...)`, fans them out to the publisher, then calls `clearRecordedEvents()` so the next behavior call starts fresh.

This pattern is sometimes called "event sourcing lite" — events are emitted reliably but not used as the source of truth for state. The aggregate still owns its state directly. We're not doing full event sourcing.

## Implementation

Two files this step. Take them in order — `Abstract` first because `Episode` depends on it.

### `Abstract.java`

`Abstract` is an entity *inside* the `Episode` aggregate. It has identity (an `AbstractId`) — two abstracts with the same text but different IDs are different abstracts — but it lives entirely inside Episode's boundary. Outside callers never get an `Abstract` reference; they always go through `Episode.theAbstract()` or hit a query.

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import java.time.Instant;

public record Abstract(AbstractId id, AbstractText text, Instant submittedAt) {

    public Abstract {
        if (id == null) {
            throw new IllegalArgumentException("Abstract id must not be null");
        }
        if (text == null) {
            throw new IllegalArgumentException("Abstract text must not be null");
        }
        if (submittedAt == null) {
            throw new IllegalArgumentException("Abstract submittedAt must not be null");
        }
    }
}
```

We're using a `record` even though Abstract is *technically* an entity (it has identity). That's a calculated cheat: inside the aggregate boundary, structural equality is acceptable because the aggregate is the only thing that creates or replaces Abstracts. Outside the boundary, two Abstract references don't exist for callers to compare. If this rule changes later, we'd promote it to a class. For now, the record buys us conciseness with no observable difference.

### `Episode.java` — the skeleton

This is the longest file you'll write in the whole workshop, so we'll do it in pieces. **Type it all into one file** — the breakdown is just for narration.

#### Fields and constructor

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Episode {

    private final EpisodeId id;
    private final EpisodeNumber number;
    private final EpisodeTitle title;
    private final AirDate airDate;
    private Abstract theAbstract;
    private final Set<PersonId> presenters = new HashSet<>();
    private final Set<PersonId> speakers = new HashSet<>();
    private EpisodeStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> recordedEvents = new ArrayList<>();

    private Episode(EpisodeId id, EpisodeNumber number, EpisodeTitle title,
                    AirDate airDate, EpisodeStatus status, Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.airDate = airDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    // ... continued below
}
```

Notice what's final and what isn't:

- **`final`**: `id`, `number`, `title`, `airDate`, `createdAt` — these don't change after construction (an episode's identity is its `id`; the rest of these are scheduling decisions). Also the collections themselves (`presenters`, `speakers`, `recordedEvents`) — their *contents* change but the references don't.
- **mutable**: `theAbstract`, `status`, `updatedAt` — these change as the lifecycle progresses.

The private constructor sets the always-present fields but does **not** populate `theAbstract` or the sets. Both factories deal with those.

#### The `schedule(...)` factory

```java
    public static Episode schedule(EpisodeNumber number, EpisodeTitle title, AirDate airDate) {
        if (airDate.value().isBefore(LocalDate.now())) {
            throw new AirDateInPast(airDate);
        }
        Instant now = Instant.now();
        Episode episode = new Episode(
            EpisodeId.random(), number, title, airDate,
            EpisodeStatus.SCHEDULED, now, now);
        episode.recordedEvents.add(new EpisodeScheduled(episode.id, number, airDate, now));
        return episode;
    }
```

Three things happen, in order:

1. **Temporal rule check** — `airDate` must not be in the past. Today is OK (same-day broadcasts happen). This is the temporal rule we left out of the `AirDate` VO in Step 1, finally checked here where we have access to "now."
2. **Construct** — mint a fresh `EpisodeId`, start in `SCHEDULED`, set both timestamps to the same `now`.
3. **Record the event** — `EpisodeScheduled` goes into `recordedEvents`. The aggregate is now responsible for telling the world it was created.

What's *not* here: the cross-aggregate "is this episode number already taken?" check. That requires reading state outside this aggregate — the database, basically — and the aggregate has no way to do that. The application service handles that check before calling `schedule(...)`. We'll see how in Step 8.

#### The `rehydrate(...)` factory

```java
    public static Episode rehydrate(EpisodeId id, EpisodeNumber number, EpisodeTitle title,
                                    AirDate airDate, Abstract theAbstract,
                                    Set<PersonId> presenters, Set<PersonId> speakers,
                                    EpisodeStatus status, Instant createdAt, Instant updatedAt) {
        Episode episode = new Episode(id, number, title, airDate, status, createdAt, updatedAt);
        episode.theAbstract = theAbstract;
        episode.presenters.addAll(presenters);
        episode.speakers.addAll(speakers);
        return episode;
    }
```

This is the *infrastructure* factory. It takes everything the persistence adapter remembered, reconstructs the aggregate, and **records no event**. The Javadoc on this method in the solution emphasizes: application code MUST NOT call this. It is for the persistence layer only.

The `addAll` calls are defensive copies in the sense that the aggregate's internal sets are isolated from whatever set the caller passed. If the caller mutates their set later, the aggregate is unaffected. (We don't *fully* defensively copy with a new `HashSet<>(...)` because the caller's set is `Set<PersonId>` of value objects — those are immutable, so we'd be defensively copying a collection of already-immutable things.)

#### Accessors and the recorded-events buffer

```java
    public EpisodeId id() { return id; }
    public EpisodeNumber number() { return number; }
    public EpisodeTitle title() { return title; }
    public AirDate airDate() { return airDate; }
    public Abstract theAbstract() { return theAbstract; }
    public Set<PersonId> presenters() { return Collections.unmodifiableSet(presenters); }
    public Set<PersonId> speakers() { return Collections.unmodifiableSet(speakers); }
    public EpisodeStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public List<DomainEvent> recordedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recordedEvents));
    }

    public void clearRecordedEvents() {
        recordedEvents.clear();
    }
}
```

Accessors are named after the property — `id()`, not `getId()`. There's no reason to prefix with `get` (we're not implementing a JavaBean spec), and the property-name style reads better in tests (`assertEquals(numberOne, episode.number())` vs `assertEquals(numberOne, episode.getNumber())`).

`presenters()` and `speakers()` return `Collections.unmodifiableSet(...)` — the caller can read but not mutate. Same for `recordedEvents()`, which additionally takes a **defensive copy** before wrapping (so the caller can't even see in-flight mutations from a concurrent behavior call).

`clearRecordedEvents()` is the only setter-shaped method on the aggregate that *isn't* a domain behavior. It exists for the application layer to call after publishing.

## Testing

Place all of these in `EpisodeTest.java` under `src/test/java/.../programming/domain/`. We're going to use JUnit's `@Nested` classes to group tests by behavior method — this becomes valuable in Step 5 and 6 when we have six behavior methods to test.

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EpisodeTest {

    private static final EpisodeNumber numberOne = new EpisodeNumber(1);
    private static final EpisodeTitle titlePilot = new EpisodeTitle("Pilot");
    private static final AirDate today = new AirDate(LocalDate.now());
    private static final AirDate yesterday = new AirDate(LocalDate.now().minusDays(1));
    private static final AirDate tomorrow = new AirDate(LocalDate.now().plusDays(1));

    @Nested
    class Schedule {

        @Test
        void factorySetsScheduledStatus() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertEquals(EpisodeStatus.SCHEDULED, episode.status());
        }

        @Test
        void factoryAssignsId() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertNotNull(episode.id());
        }

        @Test
        void factoryStoresNumberTitleAirDate() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertEquals(numberOne, episode.number());
            assertEquals(titlePilot, episode.title());
            assertEquals(tomorrow, episode.airDate());
        }

        @Test
        void factoryStartsWithoutAbstract() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertNull(episode.theAbstract());
        }

        @Test
        void factoryStartsWithEmptyPresentersAndSpeakers() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertTrue(episode.presenters().isEmpty());
            assertTrue(episode.speakers().isEmpty());
        }

        @Test
        void factoryRecordsEpisodeScheduledEvent() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            List<DomainEvent> events = episode.recordedEvents();
            assertEquals(1, events.size());
            EpisodeScheduled e = assertInstanceOf(EpisodeScheduled.class, events.get(0));
            assertEquals(episode.id(), e.episodeId());
            assertEquals(numberOne, e.number());
            assertEquals(tomorrow, e.airDate());
        }

        @Test
        void factoryAcceptsAirDateToday() {
            Episode episode = Episode.schedule(numberOne, titlePilot, today);
            assertEquals(EpisodeStatus.SCHEDULED, episode.status());
        }

        @Test
        void factoryRejectsAirDateInPast() {
            assertThrows(AirDateInPast.class,
                () -> Episode.schedule(numberOne, titlePilot, yesterday));
        }

        @Test
        void clearRecordedEventsEmptiesTheList() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            assertEquals(1, episode.recordedEvents().size());
            episode.clearRecordedEvents();
            assertTrue(episode.recordedEvents().isEmpty());
        }

        @Test
        void recordedEventsReturnsCopy() {
            Episode episode = Episode.schedule(numberOne, titlePilot, tomorrow);
            List<DomainEvent> snapshot = episode.recordedEvents();
            assertThrows(UnsupportedOperationException.class, () -> snapshot.add(null));
        }
    }
}
```

Ten tests, all under the `Schedule` nested class. Each one isolates one fact about the factory: status, id, fields, missing abstract, empty sets, event payload, today-OK, past-rejected, clear, immutable view.

`@Nested` does two things here: it groups related tests in the output (you'll see `EpisodeTest$Schedule` as a logical group), and it lets us add `@Nested SubmitAbstract`, `@Nested GoLive`, etc. in later steps without disturbing existing tests.

Run just this test class:

```bash
./mvnw test -Dtest='EpisodeTest'
```

There's also one test you might be tempted to add — `rehydrateRecordsNoEvent` — and we'll add it next step when there are more behaviors that record events, so the contrast is meaningful.

## You should now see

After Step 4, `./mvnw test` reports:

```
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

(33 from earlier + 10 new from `EpisodeTest$Schedule`.)

You have a working `Episode` aggregate that you can `schedule(...)` but can't yet do anything else with — it has no abstract, no presenters, no speakers, no way to go live or publish or cancel.

**Next:** Step 5 — Behaviors I, Content. We'll add `submitAbstract`, `assignPresenter`, and `assignSpeaker` to the aggregate.
