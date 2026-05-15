# Step 5: Behaviors I — Content

Episode now exists but it's frozen — you can `schedule` one, but you can't tell it anything else. In this step we add the three behaviors that fill an episode with content: `submitAbstract`, `assignPresenter`, `assignSpeaker`. After this step, an episode is ready to go live (Step 6 will let it).

**Time:** ~25 min · **Files modified:** 1 (`Episode.java`) · **Tests added:** ~20

## TL;DR

Add three methods to `Episode.java`:

```java
public void submitAbstract(AbstractText text) { /* status check, create Abstract, record event */ }
public void assignPresenter(PersonId personId) { /* idempotent set add, record event only if new */ }
public void assignSpeaker(PersonId personId)  { /* idempotent set add, record event only if new */ }
```

Each enforces the same shape: null-check → status-check → mutate state → record event → update `updatedAt`.

## Learning Objectives

By the end of this step you will be able to:

- Place behavior on the aggregate that owns the state being changed (the central DDD principle)
- Implement idempotent operations and explain when they're worth the cost
- Decide what state transitions allow each operation and encode that in the status check
- Write nested-class tests that group all assertions about one behavior method

## Why This Matters

This is the moment DDD pays off. In an anemic-CRUD codebase, "submit an abstract" looks like:

```java
@PostMapping("/episodes/{id}/abstract")
public Episode submitAbstract(@PathVariable UUID id, @RequestBody String text) {
    Episode e = repo.findById(id).orElseThrow();
    if (e.getStatus() != EpisodeStatus.SCHEDULED) throw new ConflictException();
    if (text == null || text.length() < 100) throw new BadRequestException();
    e.setAbstractText(text);
    e.setAbstractSubmittedAt(Instant.now());
    e.setUpdatedAt(Instant.now());
    repo.save(e);
    kafkaTemplate.send("abstract-submitted", new AbstractSubmittedEvent(id));
    return e;
}
```

Every rule that defines what "submit an abstract" means is in the controller. **Move that controller to a CLI handler, a message consumer, or a scheduled job, and every rule has to be re-implemented.** Worse, six months later somebody adds an admin endpoint that bypasses one of the checks, and now your rules quietly diverge.

In DDD, the controller becomes:

```java
service.submitAbstract(new SubmitAbstractCommand(id, text));
```

…and the rules live on `Episode.submitAbstract(AbstractText)`. Every entry point — controller, message consumer, scheduled job, admin tool — calls the same method, gets the same rules, can't bypass them.

### The shape of a behavior method

Every behavior method on `Episode` follows the same shape:

1. **Argument validation** — null checks, simple structural checks. Throws `IllegalArgumentException` (translated to 400 at the boundary).
2. **State check** — is the aggregate in a status that allows this operation? Throws `IllegalEpisodeTransition` (translated to 409 at the boundary).
3. **Higher-order precondition checks** — anything else? (Most don't have this.)
4. **Mutate state** — atomic with respect to the aggregate.
5. **Update `updatedAt`** — record when the change happened.
6. **Record the event.**

Steps 1–3 are the "refuse to do bad things" half. Steps 4–6 are the "do the thing" half. The order matters: never start mutating before all the checks pass, because there's no transaction to roll back. The aggregate either fully accepts the call or fully refuses it.

### Idempotence — when, why

`assignPresenter` is idempotent: assigning the same person twice does *not* emit a duplicate event. Why bother?

- **Retries** — REST clients retry on timeouts. If the first call succeeded but the response was lost, the retry shouldn't double-count.
- **Concurrent calls** — two admins clicking "Add me as host" at nearly the same moment shouldn't yield two events.
- **Event sourcing replay** — if we later move to event-sourced state, replaying events shouldn't accumulate duplicates.

The cost: a small extra `Set.add(...)`-returning-`boolean` check. Worth it.

`submitAbstract` is **not** idempotent — calling it twice replaces the abstract with a fresh one (new `AbstractId`, new `submittedAt`). That's a deliberate product choice: re-submitting *is* the intended way to update the abstract before going live, so each call should be a real new event. If the same text is submitted twice, that's still two distinct submissions in audit history.

## Implementation

Open `Episode.java` and add these three methods inside the class body (after `clearRecordedEvents()` and before the closing `}`).

### `submitAbstract(AbstractText)`

```java
public void submitAbstract(AbstractText text) {
    if (text == null) {
        throw new IllegalArgumentException("AbstractText must not be null");
    }
    if (status != EpisodeStatus.SCHEDULED) {
        throw new IllegalEpisodeTransition(status);
    }
    Instant now = Instant.now();
    Abstract a = new Abstract(AbstractId.random(), text, now);
    this.theAbstract = a;
    this.updatedAt = now;
    recordedEvents.add(new AbstractSubmitted(id, a.id(), now));
}
```

Walk through it:

- **Null check on the argument.** This is a real risk because callers (controllers, services) may pass `null` for an unset field. The text *content* is already validated by the `AbstractText` VO; here we only need to confirm `text` isn't `null`.
- **Status check.** Abstracts can only be submitted while `SCHEDULED`. Once live, the abstract is locked. Why? It's a product call: viewers should see the same description they saw when the episode aired.
- **Construct a new `Abstract`.** Note `AbstractId.random()` — a fresh ID each time. Submitting again later creates a new Abstract with a new ID; the previous one is discarded. The `submittedAt` is when *this* submission happened, not when the first ever was.
- **Mutate `theAbstract` and `updatedAt`.**
- **Record `AbstractSubmitted`.** The event carries the episode ID and the abstract ID — *not* the abstract text. (See Step 2 for why.)

### `assignPresenter(PersonId)`

```java
public void assignPresenter(PersonId personId) {
    if (personId == null) {
        throw new IllegalArgumentException("PersonId must not be null");
    }
    if (status != EpisodeStatus.SCHEDULED && status != EpisodeStatus.LIVE) {
        throw new IllegalEpisodeTransition(status);
    }
    if (presenters.add(personId)) {
        Instant now = Instant.now();
        this.updatedAt = now;
        recordedEvents.add(new PresenterAssigned(id, personId, now));
    }
}
```

Differences from `submitAbstract`:

- **Status check allows two states.** Presenters can be assigned both before and during live (someone joins mid-show). After publication or cancellation, no.
- **`Set.add(...)` returns `true` only if the set didn't already contain the element.** This is how we get idempotence cheaply: if the person was already a presenter, `add` returns `false`, we skip the `updatedAt` update and skip recording the event. The set's membership rule does the dedup work.

### `assignSpeaker(PersonId)`

```java
public void assignSpeaker(PersonId personId) {
    if (personId == null) {
        throw new IllegalArgumentException("PersonId must not be null");
    }
    if (status != EpisodeStatus.SCHEDULED && status != EpisodeStatus.LIVE) {
        throw new IllegalEpisodeTransition(status);
    }
    if (speakers.add(personId)) {
        Instant now = Instant.now();
        this.updatedAt = now;
        recordedEvents.add(new SpeakerAssigned(id, personId, now));
    }
}
```

A copy of `assignPresenter`, swapping `presenters` → `speakers` and `PresenterAssigned` → `SpeakerAssigned`. Two near-identical methods. If this nags at you — "shouldn't I extract a helper?" — resist. The two operations are conceptually distinct (presenters host, speakers guest), and a shared helper would have to take a flag or a `Set<PersonId> targetSet` parameter, which obscures more than it saves. **The DRY principle doesn't apply to code that happens to look the same but means different things.**

## Testing

Add three new `@Nested` classes to `EpisodeTest.java`: `SubmitAbstract`, `AssignPresenter`, `AssignSpeaker`. Total ~20 new tests.

A helper, used by tests that need an episode in `LIVE` status with all preconditions for publishing satisfied:

```java
private static Episode liveEpisodeReadyToPublish() {
    Episode episode = Episode.schedule(numberOne, titlePilot, today);
    episode.submitAbstract(new AbstractText("a".repeat(150)));
    episode.assignPresenter(PersonId.random());
    episode.assignSpeaker(PersonId.random());
    episode.goLive();
    return episode;
}
```

(`goLive` doesn't exist yet — you'll add it in Step 6. The compiler will yell about this method until then. You can either comment out tests that use the helper for now, or add a one-line stub `public void goLive() { status = EpisodeStatus.LIVE; }` to make the compiler happy and let the tests fail loudly. We recommend the second — failing tests are more honest than commented-out ones.)

### `@Nested class SubmitAbstract`

```java
@Nested
class SubmitAbstract {

    private static final AbstractText sample =
        new AbstractText("This is the abstract for the pilot episode. ".repeat(3));

    @Test void setsTheAbstract() { /* schedule, submitAbstract, assertNotNull and check text */ }
    @Test void recordsAbstractSubmittedEvent() { /* clear, submit, assert one event of right type with right fields */ }
    @Test void replacesAnExistingAbstract() { /* submit twice, assert second AbstractId differs from first */ }
    @Test void rejectsNullText() { /* assertThrows IllegalArgumentException */ }
    @Test void rejectsCallWhenLive() { /* go live, expect IllegalEpisodeTransition on submitAbstract */ }
    @Test void rejectsCallWhenPublished() { /* publish, expect IllegalEpisodeTransition */ }
    @Test void rejectsCallWhenCanceled() { /* cancel, expect IllegalEpisodeTransition */ }
}
```

The full versions are in `module-01-solution/src/test/java/.../EpisodeTest.java` — refer to it if you want exact code. The pattern is identical to the `Schedule` block from Step 4: one assertion per test, named after what it checks.

### `@Nested class AssignPresenter` and `AssignSpeaker`

Each has seven tests covering the same shape:

- `addsThePresenter` / `addsTheSpeaker`
- `recordsPresenterAssignedEvent` / `recordsSpeakerAssignedEvent`
- `isIdempotentAndDoesNotEmitDuplicateEvent` — **this is the one to write carefully.** Assign once, clear events, assign the same person again, assert no new event and the set still has size 1.
- `allowedWhileLive` — assign a third presenter when the episode is already live.
- `rejectsCallWhenPublished`
- `rejectsCallWhenCanceled`
- `rejectsNullPersonId`

Run the new tests:

```bash
./mvnw test -Dtest='EpisodeTest'
```

## You should now see

After Step 5, `./mvnw test` reports approximately:

```
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

(43 from earlier + ~20 new content-behavior tests, plus 1 if you added the temporary `goLive` stub.)

Your `Episode` now has four behaviors total: `schedule`, `submitAbstract`, `assignPresenter`, `assignSpeaker`. You can build up an episode end-to-end with content, but the lifecycle transitions are still missing.

**Next:** Step 6 — Behaviors II, Lifecycle. We'll add `goLive`, `publish`, and `cancel`. After Step 6 the aggregate is complete.
