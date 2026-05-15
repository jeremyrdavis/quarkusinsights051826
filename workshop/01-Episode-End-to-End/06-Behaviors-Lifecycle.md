# Step 6: Behaviors II — Lifecycle

The aggregate has content but no movement. In this step we add the three transitions that drive the lifecycle: `goLive` (SCHEDULED → LIVE), `publish` (LIVE → PUBLISHED), and `cancel` (SCHEDULED → CANCELED). After this, the state machine is complete.

**Time:** ~25 min · **Files modified:** 1 (`Episode.java`) · **Tests added:** ~17

## TL;DR

Three more methods on `Episode.java`:

```java
public void goLive()  { /* must be SCHEDULED, airDate must be today-or-earlier */ }
public void publish() { /* must be LIVE, must have abstract+presenter+speaker */ }
public void cancel(String reason) { /* must be SCHEDULED, reason non-blank */ }
```

Each is a transition with strict preconditions. `publish` enforces the four-rule invariant that makes the lifecycle meaningful.

## Learning Objectives

By the end of this step you will be able to:

- Implement state transitions as explicit methods (not setter calls)
- Encode multi-rule preconditions with the right typed exception per rule
- Justify *terminal states* and the absence of "un-cancel"
- Test a state machine without combinatorial explosion

## Why This Matters

A status field by itself is just data — `status = "PUBLISHED"` is the same string-typing that gave anemic-CRUD its bad name. What makes an `EpisodeStatus` actually mean something is that **only certain methods can change it, and only in certain directions.**

In an anemic model, `episode.setStatus(EpisodeStatus.PUBLISHED)` is a public method. Anyone with a reference to the entity can mark it published, with or without an abstract, with or without speakers. The rules live in whatever code happens to call the setter, scattered across the codebase.

In DDD, the *only* way `status` becomes `PUBLISHED` is through `Episode.publish()`. And `publish()` enforces four invariants:

1. Status is `LIVE` (not still SCHEDULED, not CANCELED)
2. An abstract has been submitted
3. At least one presenter is assigned
4. At least one speaker is assigned

Try to skip any of those, and you get a typed exception telling you precisely what's missing. The rules are *in one place*, and they can't be bypassed because there is no other way to mutate `status`.

### Terminal states are real

`PUBLISHED` and `CANCELED` are terminal in our domain — there is no method that transitions out of either. We explicitly chose not to add `unpublish()` or `unCancel()`. Why?

Because making them reversible is a *huge* product decision in disguise. "Unpublish" would mean removing an episode from the public catalog — which has subscribers, search indexes, social media posts. The right model isn't "unpublish"; it's "publish a correction" or "publish a retraction" — both of which are *new actions* with their own events, not reversals of old ones.

Designing out the reversal closes a door that would otherwise be a permanent source of "but what if…" discussions. **The aggregate's state machine reflects the business reality. The business doesn't unpublish, so neither does the code.**

If the business later starts unpublishing for real, the change is explicit — a new method, a new event, a new piece of behavior — not a quiet flip in an admin tool.

## Implementation

Three methods, in order. Same file (`Episode.java`), added after the content behaviors from Step 5.

### `goLive()`

```java
public void goLive() {
    if (status != EpisodeStatus.SCHEDULED) {
        throw new IllegalEpisodeTransition(status);
    }
    if (airDate.value().isAfter(LocalDate.now())) {
        throw new AirDateNotYetReached(airDate);
    }
    Instant now = Instant.now();
    this.status = EpisodeStatus.LIVE;
    this.updatedAt = now;
    recordedEvents.add(new EpisodeWentLive(id, now));
}
```

Two preconditions:

- **Status must be `SCHEDULED`.** Going live from `LIVE` or `PUBLISHED` is a programmer error. Cancellation is final.
- **Air date must be today or earlier.** This is the second half of the temporal rule we split off the VO in Step 1. `Episode.schedule(...)` ensures the date isn't in the past; `Episode.goLive()` ensures we don't go live *before* the scheduled date. Together they form a constraint pair: scheduled date ≥ today, live date ≥ scheduled date.

If both pass, set status to `LIVE`, update timestamp, record `EpisodeWentLive`.

### `publish()`

```java
public void publish() {
    if (status != EpisodeStatus.LIVE) {
        throw new IllegalEpisodeTransition(status);
    }
    if (theAbstract == null) {
        throw new MissingAbstract(id);
    }
    if (presenters.isEmpty()) {
        throw new MissingPresenter(id);
    }
    if (speakers.isEmpty()) {
        throw new MissingSpeaker(id);
    }
    Instant now = Instant.now();
    this.status = EpisodeStatus.PUBLISHED;
    this.updatedAt = now;
    recordedEvents.add(new EpisodePublished(id, number, now));
}
```

Four preconditions, each with its own typed exception. The order matters — *only when checks pass do we mutate state* — and the per-rule exception is what makes the REST layer (Step 10) able to return precise diagnostics. A "what's missing" indicator can iterate through these one at a time and tell the user "you're missing the abstract" rather than "validation failed."

A subtle point: **`publish()` does not check that the air date has passed.** That's already enforced transitively — to reach `LIVE` you had to call `goLive()`, which enforced it. We trust the state machine: anything in `LIVE` has already had its air date checked. Don't re-validate state you can already trust.

The `EpisodePublished` event carries the episode `number` in addition to the ID. The reason was foreshadowed in Step 2: publication fans out to many subscribers, and most of them want the number for display. Putting it in the event payload amortizes the cost.

### `cancel(String)`

```java
public void cancel(String reason) {
    if (reason == null || reason.isBlank()) {
        throw new IllegalArgumentException("Cancel reason must not be null or blank");
    }
    if (status != EpisodeStatus.SCHEDULED) {
        throw new IllegalEpisodeTransition(status);
    }
    Instant now = Instant.now();
    this.status = EpisodeStatus.CANCELED;
    this.updatedAt = now;
    recordedEvents.add(new EpisodeCanceled(id, reason, now));
}
```

- **Reason check.** A free-form string but it must be non-blank. (We discussed in Step 2 why this isn't a VO yet: there's no business rule about cancellation reasons beyond "must say something.")
- **Status check.** Only `SCHEDULED` episodes can be canceled. Once live or published, the show happened — we don't pretend otherwise. (If the show was awful and we want to retract it: that's a *different action* — publish a retraction event.)

## Testing

Add three more `@Nested` classes: `GoLive`, `Publish`, `Cancel`. About 17 new tests total.

The `liveEpisodeReadyToPublish()` helper from Step 5 (which previously needed a stub `goLive`) now works with the real implementation — go back and remove the stub if you added one.

A test that exists for the first time now that we have multiple events emitted: **`rehydrateRecordsNoEvent`**. Put it in the `Schedule` block from Step 4:

```java
@Test
void rehydrateRecordsNoEvent() {
    Episode rehydrated = Episode.rehydrate(
        EpisodeId.random(), numberOne, titlePilot, today,
        null,                       // no abstract
        Collections.emptySet(),     // no presenters
        Collections.emptySet(),     // no speakers
        EpisodeStatus.SCHEDULED,
        Instant.now(), Instant.now()
    );
    assertTrue(rehydrated.recordedEvents().isEmpty());
}
```

This is the test that pins down the contract: **rehydration is not a domain action, so it must not record an event.** If someone refactors `rehydrate` later and adds a misplaced `recordedEvents.add(...)`, this test catches it.

### `@Nested class GoLive`

Six tests. Refer to `module-01-solution/src/test/java/.../EpisodeTest.java` for the exact code. The shape:

- `transitionsToLive` — happy path
- `recordsEpisodeWentLiveEvent`
- `rejectsCallBeforeAirDate` — expect `AirDateNotYetReached`
- `rejectsCallWhenAlreadyLive` — expect `IllegalEpisodeTransition`
- `rejectsCallWhenPublished`
- `rejectsCallWhenCanceled`

### `@Nested class Publish`

Around six tests. The four "missing X" tests are the load-bearing ones — each one removes exactly one precondition and asserts the corresponding exception:

```java
@Test
void rejectsCallWhenMissingAbstract() {
    Episode episode = Episode.schedule(numberOne, titlePilot, today);
    episode.assignPresenter(PersonId.random());
    episode.assignSpeaker(PersonId.random());
    episode.goLive();
    assertThrows(MissingAbstract.class, () -> episode.publish());
}
```

Repeat with `MissingPresenter` and `MissingSpeaker` by removing the corresponding precondition setup. The fourth case is `IllegalEpisodeTransition` when status is wrong.

### `@Nested class Cancel`

About five tests:

- `transitionsToCanceled`
- `recordsEpisodeCanceledEventWithReason` — assert the `reason` field on the event matches
- `rejectsBlankReason`
- `rejectsCallWhenLive`
- `rejectsCallWhenPublished`

Run all:

```bash
./mvnw test -Dtest='EpisodeTest'
```

## You should now see

After Step 6, `./mvnw test` reports approximately:

```
[INFO] Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

(64 from earlier + ~17 lifecycle tests.)

You now have a **complete `Episode` aggregate.** It can be:

- created via `schedule(...)`
- filled with content via `submitAbstract`, `assignPresenter`, `assignSpeaker`
- transitioned via `goLive`, `publish`, `cancel`
- loaded from persistence via `rehydrate(...)`

…and every illegal combination of those calls throws a precise, typed exception. The "rich domain model" half of DDD is done. The next four steps are about **wiring that aggregate into a Quarkus app** without losing the boundaries we just built.

**Next:** Step 7 — the `EpisodeRepository` port and command records. We start crossing the boundary from domain to application.
