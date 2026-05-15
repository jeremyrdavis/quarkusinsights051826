# Overview: The QuarkusInsights Podcast Domain

Before diving into Step 1, here's a one-page tour of the business problem the workshop's app solves. Knowing what an "Episode" actually *is* makes the technical decisions you're about to make a lot easier to follow.

## The product

**QuarkusInsights** is a tech podcast / live stream. The team publishes one show at a time. Each show has:

- A **number** (episode 1, 2, 3, …) — sequential, unique
- A **title** ("Domain-Driven Design and Hexagonal Architecture")
- An **air date** — the day the show streams
- An **abstract** — the synopsis on the public catalog page
- One or more **presenters** (hosts who run the show)
- One or more **speakers** (guests who appear on it)

Episodes go through a clear lifecycle:

```
   ┌─────────────┐         ┌──────┐         ┌───────────┐
   │  SCHEDULED  │ ──goLive──>  │ LIVE │ ──publish──>  │ PUBLISHED │
   └─────────────┘         └──────┘         └───────────┘
          │
          │ cancel
          ▼
   ┌──────────┐
   │ CANCELED │   (terminal)
   └──────────┘
```

That's the entire state machine you'll encode in code. Four states, four transitions. PUBLISHED and CANCELED are terminal — there's no "un-publish" or "un-cancel."

## The business rules

Roughly twelve rules govern what can and can't happen to an Episode. You'll encode every one of them — in the right place — as you build the aggregate.

### Scheduling
1. Episode numbers start at 1.
2. Two Episodes can't have the same number.
3. You can't schedule an Episode in the past (today is OK for same-day shows).

### Content
4. An abstract must be at least 100 characters and at most 5000.
5. Titles must be 1–200 characters and non-blank.
6. An abstract can be replaced (re-submitted) any time before going live.
7. Once live, the abstract is locked.
8. The same person can't be assigned twice in the same role on the same episode (idempotent).

### Lifecycle
9. Going live requires today ≥ scheduled air date.
10. Publishing requires status = LIVE, an abstract, ≥1 presenter, and ≥1 speaker.
11. Cancellation is only allowed before the show goes live.
12. Cancellation requires a non-blank reason.

Some of these are **value-object rules** (#1, #4, #5 — they depend only on the value itself), some are **aggregate rules** (#3, #7, #8, #9, #10, #11, #12 — they depend on aggregate state or "now"), and one is a **cross-aggregate rule** (#2 — uniqueness across all Episodes, which no single aggregate can verify). DDD says each rule lives in the right place; the workshop walks you through deciding which is which.

## Why "QuarkusInsights" specifically

The talk this workshop accompanies is itself a *QuarkusInsights episode* — a tech podcast about Quarkus, DDD, hexagonal architecture, and agentic AI. The reference app *is the show's own management system*. So Episode 1 in the demo is the show you're watching. It's mildly recursive in a way that we hope is endearing.

If you want to see the full app — including the People, Engagement (comments + ratings), and Catalog contexts that build on top of the Episode aggregate you're about to construct — it's at `../../quarkusinsightsddd/`. After this workshop, that codebase will read like an obvious extension of the same patterns.

## The bounded contexts (one to four)

The full reference app uses four bounded contexts:

- **Programming** — the Episode aggregate. This is what we build in the workshop.
- **People** — Person aggregate. Tracks who hosts and guests are (Episodes reference them by `PersonId`).
- **Engagement** — Comment + Rating aggregates. Public engagement after publication.
- **Catalog** — denormalized read model + CQRS projectors. The public-facing pages.

In the workshop, **only Programming is coded**. You'll see `PersonId` references in the Episode aggregate (presenters and speakers are `Set<PersonId>`), but `PersonId` lives in the shared kernel — you don't need to know what a `Person` is to model that an Episode has presenters.

This separation is intentional and is one of the more powerful DDD ideas. Aggregates from different contexts communicate only through IDs and domain events, never by direct reference. That's what makes the architecture withstand growth: you can add a fourth context later without rewriting the first.

## Tech stack

Just so you have the names handy:

- **Quarkus 3.35.2** on **Java 25**
- **Hibernate ORM with Panache** (repository pattern, not active record)
- **PostgreSQL** via Quarkus **Dev Services** (no manual datasource config)
- **JAX-RS** for REST endpoints, **Jackson** for JSON
- **CDI** for dependency injection and event dispatch

The starter `pom.xml` already has every extension you need. You won't add Maven dependencies during the workshop.

## What's next

Now you know what we're building and why. Open [01-Value-Objects.md](01-Value-Objects.md) and let's start.
