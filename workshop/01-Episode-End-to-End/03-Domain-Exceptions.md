# Step 3: Domain Exceptions

The aggregate we're about to build (Step 4) refuses to do bad things — but it has to communicate *why* it refused. That's what these eight exception classes are for. Each one names a specific rule. Each one carries the context a REST handler will need to translate it into a 400 or 409 later.

**Time:** ~20 min · **Files created:** 8 · **Tests added:** 0 (covered indirectly by aggregate tests)

## TL;DR

Create eight classes in `programming/domain/`, all extending `RuntimeException`. Each carries the identifier of the thing it's complaining about:

```java
public class EpisodeNumberAlreadyExists extends RuntimeException {
    private final EpisodeNumber number;
    public EpisodeNumberAlreadyExists(EpisodeNumber number) {
        super("Episode number " + number.value() + " is already in use");
        this.number = number;
    }
    public EpisodeNumber number() { return number; }
}
```

…plus `EpisodeNotFound`, `MissingAbstract`, `MissingPresenter`, `MissingSpeaker`, `IllegalEpisodeTransition`, `AirDateInPast`, `AirDateNotYetReached`.

## Learning Objectives

By the end of this step you will be able to:

- Explain why domain rules surface as typed exceptions, not booleans or `Optional`s
- Name an exception after a *rule*, not an *operation*
- Decide what context to carry in an exception payload
- Justify why these are `RuntimeException`s, not checked exceptions

## Why This Matters

There are roughly three ways a domain operation can refuse:

1. **Return `false` or `Optional.empty()`** — and force every caller to remember to check. Easy to forget. Composes badly.
2. **Return a `Result<Episode, ErrorCode>`** — explicit, but verbose, and you've now invented a parallel exception system.
3. **Throw a typed exception** — caller can ignore (it'll bubble), catch (handle), or translate at a boundary (REST → 400/409).

For a domain layer that's called only by a thin application service (which is, in turn, called only by a REST resource), throwing wins. The REST resource has exactly one place where exceptions are turned into HTTP responses — the exception mappers we'll write in Step 10 — and that machinery already exists in JAX-RS. Adding a parallel `Result<>` shape doesn't pay off.

**Why typed and not generic?** You could throw `IllegalStateException("Episode already published")` everywhere. It compiles. The downside shows up at the REST boundary: a single `ExceptionMapper<IllegalStateException>` cannot tell which kind of illegal state happened, so it can't pick the right HTTP code (409 for lifecycle conflicts vs 422 for validation conflicts) or include the right context in the response body. **One exception type per rule** means the mapper layer can be precise.

### What goes in an exception's payload

Each exception carries the identifier of the offending thing:

- `EpisodeNumberAlreadyExists` carries the `EpisodeNumber` that was taken
- `MissingAbstract` / `MissingPresenter` / `MissingSpeaker` carry the `EpisodeId` that's missing the prerequisite
- `IllegalEpisodeTransition` carries the `EpisodeStatus` the episode was actually in
- `AirDateInPast` / `AirDateNotYetReached` carry the offending `AirDate`

Why bother? Because **a stack trace is enough for a developer, but not for a user.** When the REST resource gets `IllegalEpisodeTransition`, it can return a JSON body like `{"error": "illegal-transition", "currentStatus": "PUBLISHED"}` — the front-end can show a sensible message ("This episode is already published, you can't cancel it"). If the exception only carried a `String message`, the front-end has to parse English.

### Why all `RuntimeException`?

These rules are *not* recoverable conditions in the usual sense. A REST request that violates the "must have an abstract" rule isn't going to be retried by the framework. The application service doesn't have a meaningful fallback — it's going to translate the exception to a 400 or 409 and let the client decide. Java's checked-exception machinery (`throws` clauses, `try` blocks at every call site) buys us nothing here, and costs us at every method signature in between. **Unchecked is the right default for domain rule violations.**

The opposite — making them checked — is also a defensible choice some teams take, especially when the application service does have a sensible recovery path. We're optimizing for "small, focused signatures."

## Implementation

All eight in `src/main/java/io/arrogantprogrammer/quarkusinsights/programming/domain/`. Each follows the same shape: extend `RuntimeException`, accept the offending context in the constructor, expose it via a getter.

### `EpisodeNumberAlreadyExists.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

public class EpisodeNumberAlreadyExists extends RuntimeException {

    private final EpisodeNumber number;

    public EpisodeNumberAlreadyExists(EpisodeNumber number) {
        super("Episode number " + number.value() + " is already in use");
        this.number = number;
    }

    public EpisodeNumber number() {
        return number;
    }
}
```

Notice that this exception is about a **cross-aggregate** rule — "no two episodes can have the same number" — which a single `Episode` instance cannot check. We'll enforce the rule in the aggregate's `schedule(...)` factory by giving it a port (`EpisodeRepository.numberExists(...)`) and have it throw this exception. Step 4.

(In the bigger reference app there's also a database `UNIQUE` constraint backing this up. That's the "three-place rule" the design spec mentions: service check, DB constraint, exception translator. We'll get to the DB side in Step 9.)

### `EpisodeNotFound.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

public class EpisodeNotFound extends RuntimeException {

    private final EpisodeId episodeId;

    public EpisodeNotFound(EpisodeId episodeId) {
        super("Episode " + episodeId.value() + " not found");
        this.episodeId = episodeId;
    }

    public EpisodeId episodeId() {
        return episodeId;
    }
}
```

This one is *thrown by the application service* (Step 8) when a `findById` returns empty, not by the aggregate (an aggregate can't be missing — it's the thing). Putting the exception class in `programming/domain/` rather than `programming/application/` is a small judgement call: domain rules and application not-found rules are both expressed in the ubiquitous language of the context, and tucking them all in `domain/` keeps related exceptions together. Some teams prefer to put `EpisodeNotFound` next to the application service. Either is defensible.

### `MissingAbstract.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

public class MissingAbstract extends RuntimeException {

    private final EpisodeId episodeId;

    public MissingAbstract(EpisodeId episodeId) {
        super("Episode " + episodeId.value() + " has no abstract; submitAbstract() must be called before publish()");
        this.episodeId = episodeId;
    }

    public EpisodeId episodeId() {
        return episodeId;
    }
}
```

Note the message text: it's diagnostic for *developers* ("call `submitAbstract()` before `publish()`"). The end-user-facing message comes from the REST mapper in Step 10, which can decide to translate this to "Please add an abstract before publishing." Keep the exception's message dev-friendly; do user-facing copy at the boundary.

### `MissingPresenter.java` and `MissingSpeaker.java`

```java
public class MissingPresenter extends RuntimeException {
    private final EpisodeId episodeId;
    public MissingPresenter(EpisodeId episodeId) {
        super("Episode " + episodeId.value() + " has no presenters; assignPresenter() must be called before publish()");
        this.episodeId = episodeId;
    }
    public EpisodeId episodeId() { return episodeId; }
}

public class MissingSpeaker extends RuntimeException {
    private final EpisodeId episodeId;
    public MissingSpeaker(EpisodeId episodeId) {
        super("Episode " + episodeId.value() + " has no speakers; assignSpeaker() must be called before publish()");
        this.episodeId = episodeId;
    }
    public EpisodeId episodeId() { return episodeId; }
}
```

Two near-identical classes with different names. Same reasoning as in Step 2 with the `PresenterAssigned` / `SpeakerAssigned` events: presenter and speaker are distinct domain concepts even when they happen to have the same shape today.

### `IllegalEpisodeTransition.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

public class IllegalEpisodeTransition extends RuntimeException {

    private final EpisodeStatus actual;

    public IllegalEpisodeTransition(EpisodeStatus actual) {
        super("Episode is " + actual + "; the requested transition is not allowed from this state");
        this.actual = actual;
    }

    public EpisodeStatus actual() {
        return actual;
    }
}
```

This is the workhorse of the lifecycle. *Every* behavior method on the aggregate checks "am I in the right status to do this?" and throws `IllegalEpisodeTransition` if not. The exception carries the *actual* status; whichever caller catches it knows what state the episode is really in and can act accordingly (REST: 409 Conflict; UI: refresh the page to show current state).

Notice what it *doesn't* carry: the operation that was attempted. The caller already knows which method they called; they don't need to be told. This is a tiny but real cost-saver: one less field per exception, one less thing to keep correct.

### `AirDateInPast.java` and `AirDateNotYetReached.java`

```java
public class AirDateInPast extends RuntimeException {
    private final AirDate airDate;
    public AirDateInPast(AirDate airDate) {
        super("AirDate " + airDate.value() + " is in the past; episodes must be scheduled for today or later");
        this.airDate = airDate;
    }
    public AirDate airDate() { return airDate; }
}

public class AirDateNotYetReached extends RuntimeException {
    private final AirDate airDate;
    public AirDateNotYetReached(AirDate airDate) {
        super("AirDate " + airDate.value() + " has not yet arrived; the episode cannot go live until then");
        this.airDate = airDate;
    }
    public AirDate airDate() { return airDate; }
}
```

Two date-related rules, two distinct exceptions. They both say something about *time*, but they're different rules: one says "you can't schedule in the past," the other says "you can't go live before the scheduled date." Same `AirDate` carried, opposite-pointing constraints.

These are the prime example of "temporal rule lives on aggregate behavior, not on the VO" from Step 1. `AirDate` itself accepts any `LocalDate`. `Episode.schedule(...)` rejects past dates by throwing `AirDateInPast`. `Episode.goLive()` rejects future dates by throwing `AirDateNotYetReached`. The VO stays clock-independent; the aggregate methods consult the clock when they need to.

## Testing

Like the events in Step 2, exceptions get their semantics exercised indirectly through aggregate tests. There's not much to unit-test about a `RuntimeException` subclass with one field and a `toString`.

If you want, write one smoke test that confirms construction works and messages are non-blank — most teams skip this.

## You should now see

After Step 3, `./mvnw test` reports something like:

```
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Same count as Step 2. Eight more files exist in `programming/domain/`. The events from Step 2, the value objects from Step 1, and these exceptions are the entire alphabet of the domain. Everything from here on is about combining them.

If you got a compile error on `EpisodeId` or `EpisodeNumber` references inside the exceptions, check that you imported `io.arrogantprogrammer.quarkusinsights.shared.EpisodeId` (it's in `shared/`, not `programming/domain/`).

**Next:** Step 4 — the `Episode` aggregate itself. We'll build the skeleton (fields, constructor, rehydrate) and the `schedule(...)` factory method.
