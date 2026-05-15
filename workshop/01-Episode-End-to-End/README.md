# Module 01: Episode End-to-End

Build the `Episode` aggregate from value objects through REST, in ten steps. By the end of this module, you'll have a working Quarkus application that schedules podcast episodes, walks them through a lifecycle, and exposes the whole thing over HTTP — with every business rule enforced by the domain layer.

**Time:** 3–4 hours · **Steps:** 10 · **Final test count:** ~130

## Before you start

Make sure your environment is set up — see the [parent README](../README.md) for local or Codespaces options. Verify with:

```bash
cd module-01-code
./mvnw test
```

…and confirm `HealthCheckSmokeTest` passes. That's the one test the starter ships with; if it's green, the rest of the workshop will work.

## What's already done for you

The starter (`module-01-code/`) ships with the **shared kernel** pre-built:

- `shared/EpisodeId`, `PersonId`, `CommentId`, `RatingId` — UUID-wrapping value objects shared across (future) bounded contexts
- `shared/DomainEvent` — the marker interface for all domain events
- `shared/application/DomainEventPublisher` — the publish port
- `shared/application/CdiDomainEventPublisher` — production CDI implementation
- `shared/interfaces/IllegalArgumentExceptionMapper` — converts VO validation failures to HTTP 400
- `shared/application/RecordingDomainEventPublisher` (test helper) — in-memory publisher for unit tests

The starter also has empty `programming/domain/`, `programming/application/`, `programming/infrastructure/persistence/`, and `programming/interfaces/` packages waiting for you to fill in.

## The steps

| # | Topic | Time | What you build |
|---|---|---|---|
| [01](01-Value-Objects.md) | Value Objects | 20 min | `EpisodeNumber`, `EpisodeTitle`, `AbstractId`, `AbstractText`, `AirDate`, `EpisodeStatus` |
| [02](02-Domain-Events.md) | Domain Events | 15 min | 7 events from `EpisodeScheduled` to `EpisodeCanceled` |
| [03](03-Domain-Exceptions.md) | Domain Exceptions | 20 min | 8 typed exceptions: `EpisodeNumberAlreadyExists`, `MissingAbstract`, etc. |
| [04](04-Aggregate-Skeleton.md) | Aggregate Skeleton | 30 min | `Episode` + `Abstract` + `schedule` factory + `rehydrate` |
| [05](05-Behaviors-Content.md) | Behaviors I — Content | 25 min | `submitAbstract`, `assignPresenter`, `assignSpeaker` |
| [06](06-Behaviors-Lifecycle.md) | Behaviors II — Lifecycle | 25 min | `goLive`, `publish`, `cancel` |
| [07](07-Repository-Port-and-Commands.md) | Repository Port + Commands | 15 min | `EpisodeRepository` interface + 7 command records |
| [08](08-Application-Service.md) | Application Service | 30 min | `EpisodeService` with 7 `@Transactional` methods, tested in-memory |
| [09](09-Persistence-Adapter.md) | Persistence Adapter | 30 min | `EpisodeEntity` (Panache), `EpisodeMapper`, `EpisodeRepositoryImpl` |
| [10](10-REST-Adapter.md) | REST Adapter | 30 min | `EpisodeResource` + DTOs + 8 exception mappers; end-to-end smoke test |

Take a break after Step 5 or 6 — that's a natural inflection point.

## How a step is structured

Every step file follows the same shape:

1. **TL;DR** — the minimal code to copy if you're speed-running
2. **Learning Objectives** — what you'll be able to do afterward
3. **Why This Matters** — the *why* before the *what*
4. **Implementation** — annotated code blocks, file by file
5. **Testing** — the tests that pin the behavior down
6. **You should now see** — the expected test count, the confirmation the step worked

Read the TL;DR first to see where the step is going, then read **Why This Matters** before typing. Then work through **Implementation** and **Testing** in your IDE, running `./mvnw test` after each file.

## Read the domain primer first

Before Step 1, read [Overview.md](Overview.md) for a quick orientation to the QuarkusInsights podcast domain. Knowing what an "Episode" *is* in business terms makes the technical decisions easier to follow.

## When you get stuck

`module-01-solution/` is the answer key. It's the same Maven project, with the Programming context fully implemented. Compare your starter against the corresponding file in the solution:

```bash
# From module-01-code/:
diff src/main/java/io/arrogantprogrammer/quarkusinsights/programming/domain/EpisodeNumber.java \
     ../module-01-solution/src/main/java/io/arrogantprogrammer/quarkusinsights/programming/domain/EpisodeNumber.java
```

You can also run the solution's tests as a reference for what your tests should look like.

## After the workshop

The full QuarkusInsights reference app at `../../quarkusinsightsddd/` (two directories up) builds on what you just learned: People, Engagement, and Catalog bounded contexts, CQRS read models with projectors, and a Qute + HTMX UI. Same patterns, wider scope.

The talk at `../../docs/abstract.md` and `../../SHOW_NOTES.md` explains the architectural rationale and how this style supports agentic (AI-assisted) development.

Ready? Open [Overview.md](Overview.md), then [01-Value-Objects.md](01-Value-Objects.md).
