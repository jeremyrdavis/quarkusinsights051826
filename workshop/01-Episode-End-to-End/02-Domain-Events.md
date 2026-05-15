# Step 2: Domain Events

A domain event is the smallest unit of "something the business noticed happened." In this step we'll create seven of them — one for each meaningful thing an `Episode` can record about its own life. We won't publish them yet; that's Step 8. Right now we're just defining the vocabulary.

**Time:** ~15 min · **Files created:** 7 · **Tests added:** 0 (records have nothing to test alone)

## TL;DR

Create seven records in `programming/domain/`, all implementing the `DomainEvent` interface that already exists in `shared/`:

```java
public record EpisodeScheduled(
    EpisodeId episodeId,
    EpisodeNumber number,
    AirDate airDate,
    Instant occurredAt
) implements DomainEvent {}
```

…plus `AbstractSubmitted`, `PresenterAssigned`, `SpeakerAssigned`, `EpisodeWentLive`, `EpisodePublished`, `EpisodeCanceled`. Each is a record. Each carries IDs and primitives only — no aggregate references.

## Learning Objectives

By the end of this step you will be able to:

- Define a domain event as a Java record implementing a shared `DomainEvent` interface
- Explain why event payloads carry IDs, not whole aggregates
- Explain why `occurredAt` is for audit, not business logic
- Name events in the past tense and explain why

## Why This Matters

If you've worked with `ActiveRecord` or any "save and notify" framework, the term "domain event" can sound like jargon for `model.afterUpdate()`. It's not. The difference is in two words: **already happened.**

A command says "schedule this episode." It can fail, be rejected, be re-validated. A domain event says "this episode *was* scheduled." Past tense. Done. Immutable. Subscribers can react however they want — update a read model, send a notification, write to an audit log — but none of them can undo the fact.

This past-tense framing has practical consequences:

- **Events are records, never classes** — there is no behavior to define on a fact. Trying to put a method on an event ("`isUrgent()`") is a smell: it means the subscriber wanted information that should be in the payload.
- **Events carry IDs, not aggregates** — when `AbstractSubmitted` is published, the `Episode` aggregate might still be mid-transaction, or already discarded from memory. Subscribers can't reach back into the aggregate; they get what the event carries. So we put the `EpisodeId` in, not the `Episode`.
- **Events are named in the past tense** — `EpisodeScheduled`, not `ScheduleEpisode` (that's a command) and not `EpisodeSchedule` (that's nothing in particular). The naming is the type signature for "this already happened."

Notice that none of the seven events here will be *published* in this step. We're defining the vocabulary first. The aggregate (Step 4–6) will return events. The application service (Step 8) will hand them to a publisher. The publisher fans them out to CDI subscribers (Step 9 onward in the bigger reference app, though we don't add subscribers in this workshop). Right now: just the records.

### The `DomainEvent` interface (already in `shared/`)

Look at `src/main/java/io/arrogantprogrammer/quarkusinsights/shared/DomainEvent.java`:

```java
public interface DomainEvent {
    Instant occurredAt();
}
```

That's the whole contract. One method. It's a **marker interface plus a timestamp**.

The Javadoc on the method is doing the load-bearing work: `occurredAt` is for ordering and auditing, **not for business decisions**. Why? Wall-clock time on a distributed system is unreliable — clock skew between machines, replay during recovery, late arrivals from queues. If your subscriber says "if the event is more than 5 minutes old, ignore it," you've built a bug that will fire in production. Use sequence numbers, version stamps, or correlation IDs for ordering. Use `occurredAt` only for "show this in the audit log."

This is the kind of constraint that's easy to forget six months in. The way DDD enforces it isn't a comment — it's the *absence* of any other field on the interface. If subscribers could read a "sequence number" off the interface, somebody would use it for business logic. By keeping the interface minimal, we make the right thing easy and the wrong thing impossible.

## Implementation

All seven events live in `src/main/java/io/arrogantprogrammer/quarkusinsights/programming/domain/`. Each is a record implementing `DomainEvent`.

### `EpisodeScheduled.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

public record EpisodeScheduled(
    EpisodeId episodeId,
    EpisodeNumber number,
    AirDate airDate,
    Instant occurredAt
) implements DomainEvent {}
```

Why these four fields and not others? **Whatever a subscriber needs to do its job, without calling back into the aggregate.** The catalog read model wants to upsert a row keyed by `episodeId`, displaying the `number` and `airDate`. So those go in. Subscribers that wanted the title — too bad — would have to either accept that the schedule event doesn't carry title (and wait for an `AbstractSubmitted` if they need it later), or we add `title` to the payload. Each field is a contract with future subscribers.

### `AbstractSubmitted.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.DomainEvent;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

import java.time.Instant;

public record AbstractSubmitted(
    EpisodeId episodeId,
    AbstractId abstractId,
    Instant occurredAt
) implements DomainEvent {}
```

Notice what's *not* here: `AbstractText`. The body is potentially 5000 characters. Putting it in every event payload means every subscriber pays for it whether they care or not. A subscriber that needs the body can read it from the aggregate (via a query port) when it gets the event. Most subscribers (search index, notification) just need to know "this episode now has an abstract — go re-render."

This is a judgement call, not a rule. The trade-off: **smaller payload = subscribers may need extra lookups; larger payload = subscribers are self-contained but every event is heavy.** Lean small unless you have evidence otherwise.

### `PresenterAssigned.java` and `SpeakerAssigned.java`

```java
public record PresenterAssigned(
    EpisodeId episodeId,
    PersonId personId,
    Instant occurredAt
) implements DomainEvent {}

public record SpeakerAssigned(
    EpisodeId episodeId,
    PersonId personId,
    Instant occurredAt
) implements DomainEvent {}
```

Two events with identical shapes but different meanings. They're not the same event because **the business cares about the distinction** — presenter (host) and speaker (guest) play different roles on an episode, and a subscriber that displays the episode page needs to know which is which.

We *could* have used one `PersonAssignedToEpisode` event with a `role: PRESENTER|SPEAKER` enum. That works too. The reason to keep them separate: the events are dispatched to CDI observers by *type*, and a subscriber that only cares about presenters writes `void on(@Observes PresenterAssigned e)` instead of `void on(@Observes PersonAssignedToEpisode e)` and then filtering by role.

### `EpisodeWentLive.java`

```java
public record EpisodeWentLive(EpisodeId episodeId, Instant occurredAt) implements DomainEvent {}
```

The minimal event: just the ID and the timestamp. State transitions to LIVE don't carry extra payload because subscribers can read whatever they need from the aggregate by `episodeId`.

### `EpisodePublished.java`

```java
public record EpisodePublished(
    EpisodeId episodeId,
    EpisodeNumber number,
    Instant occurredAt
) implements DomainEvent {}
```

Carries `number` even though `episodeId` would be enough to look it up. Why? **Publication is the event that fans out to the most subscribers** — the public catalog, notification systems, search indexes. Every subscriber needs the number for display. Putting it in the payload saves N lookups. It's a deliberate cost-amortization choice. Smaller events earlier in the lifecycle, fatter events at the publish boundary.

### `EpisodeCanceled.java`

```java
public record EpisodeCanceled(
    EpisodeId episodeId,
    String reason,
    Instant occurredAt
) implements DomainEvent {}
```

The `reason` is a free-form `String`, not a `CancellationReason` VO. This is intentional: there's no rule about cancellation reasons (they're not enumerated, not validated, not user-facing in a strict way). A future product call could turn it into a VO with categories ("scheduling-conflict", "guest-unavailable", etc.). For now it's a `String`. Don't over-design when the rules aren't there yet.

## Testing

Events have no behavior beyond record-generated equality, so they don't need dedicated tests. Their semantics get exercised indirectly when we test the aggregate (Step 4–6) — every behavior method returns the events it emitted, and those tests assert "yes, this method emitted that event with these fields."

If you want a quick sanity check that the records compile and you got the field types right, write a one-test smoke check (this is **optional**):

```java
// src/test/java/...domain/EventsSmokeTest.java
class EventsSmokeTest {
    @Test
    void allEventsImplementDomainEvent() {
        Instant now = Instant.now();
        EpisodeId id = EpisodeId.random();
        assertTrue(new EpisodeWentLive(id, now) instanceof DomainEvent);
        // ... etc
    }
}
```

Most teams skip this and trust the compiler — `implements DomainEvent` will fail to compile if the record doesn't match the interface. Skip the smoke test unless it makes you feel better.

## You should now see

After Step 2, `./mvnw test` reports something like:

```
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Same count as Step 1 — no new tests, just seven new files in `programming/domain/`. The seven events import `DomainEvent`, `EpisodeId`, and `PersonId` from `shared/`, and the value objects you built in Step 1.

If you see a compilation error like `cannot find symbol: class EpisodeNumber`, you skipped a VO or misspelled the package. Open the matching file in `module-01-solution/` to compare.

**Next:** Step 3 — Domain Exceptions. The aggregate's behaviors raise typed exceptions for every rule they enforce. We'll define those before we write the behaviors themselves.
