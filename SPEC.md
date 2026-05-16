# QuarkusInsights — Specification

You are building a podcast / live-stream management system called **QuarkusInsights** as a single Quarkus monolith using Domain-Driven Design and Hexagonal Architecture. This file is the contract. It describes the application and the architectural rules; it does not prescribe class names, file layout idioms, or implementation details. You pick those.

The bar: **every business rule is enforced by structure, not by convention.** A maintainer should be able to delete the comments and still not be able to violate the rules. An adapter swap (REST → CLI, Panache → jOOQ) should require touching only the adapter, never the domain.

## Stack

- Quarkus 3.x (latest stable), Java 21+ — single Maven module
- Hibernate ORM with Panache (repository style, not active record)
- PostgreSQL via Quarkus Dev Services (no manual datasource config required in dev/test)
- Quarkus REST + Jackson for JSON
- Quarkus Qute + HTMX (CDN-loaded) for server-rendered UI
- JUnit 5, REST Assured

## The application

QuarkusInsights publishes one show at a time. Each show has a sequential number, a title, an air date, an abstract, one or more presenters (hosts), and one or more speakers (guests). Shows progress through this lifecycle:

```
   SCHEDULED ──goLive──> LIVE ──publish──> PUBLISHED
       │
       └─ cancel ──> CANCELED   (terminal)
```

PUBLISHED and CANCELED are terminal — there is no "unpublish" or "uncancel". An episode in those states stays there.

After publication, viewers leave comments and submit ratings. Comments are auto-approved; ratings are 1–5 stars. A public catalog page lists upcoming and past shows, shows each episode's detail (abstract, presenters, speakers, comments, average rating), and accepts new comments/ratings via HTMX fragments.

There is no authentication. Commenters and raters identify themselves with a free-form handle.

## Architectural rules

These are non-negotiable. Each rule states *what* and *why*.

- **Domain layer is pure POJOs.** No imports from `jakarta.persistence`, `jakarta.ws.rs`, `io.quarkus.*`, or any framework. *Why:* the domain is the part that must not change when the framework changes. If it imports the framework, the framework is now load-bearing for the business rules.

- **Aggregates enforce their own invariants in their own methods.** Validation does not live in REST controllers, application services, or database constraints alone. A behavior method either accepts the call and records the outcome, or refuses it with a typed exception. *Why:* if the rule lives elsewhere, it can be bypassed by any new entry point (CLI, message consumer, admin tool).

- **Persistence is an adapter.** The aggregate (a POJO) and the row-shaped persistence model are different objects, translated by an explicit mapper. The aggregate must not extend any framework base class or carry any persistence annotations. *Why:* the aggregate's lifecycle is owned by the domain, not by Hibernate. JPA's reflection-based field access and lazy-loading proxies routinely break invariants the aggregate is trying to protect.

- **REST is an adapter.** Resources translate JSON ↔ commands and translate domain exceptions to HTTP codes. They contain no branching on business state. Validation happens in value-object constructors at the boundary, not in the resource. *Why:* the same domain operation must be callable from anywhere — CLI, message consumer, scheduled job — without re-implementing rules.

- **Cross-context references are by ID only.** When the Episode aggregate refers to a Person who is a presenter, it holds that Person's ID, never a direct Person reference. *Why:* direct references defeat aggregate boundaries — a mutation through one aggregate would silently change what another aggregate sees, and consistency invariants stop being enforceable.

- **Cross-context fan-out is via domain events.** When something happens in one bounded context that another context needs to react to, the source records a domain event and a subscriber in the other context observes it. No direct cross-context service calls. *Exception:* a read-only query port (e.g., the public catalog reading display names from People) is allowed.

- **When a rule cannot be enforced by any single aggregate, enforce it in three places.** For example: "no two episodes share the same number" cannot be checked by an Episode in isolation. Enforce such a rule in (1) the application service via a repository query before creating/mutating the aggregate, (2) a database constraint as a race-protecting safety net, (3) the persistence adapter, which catches the constraint-violation exception and re-throws the domain exception so callers never see infrastructure errors. *Why:* the service check is fast and gives clean error messages; the DB constraint catches concurrent races; the adapter translation keeps the domain's exception vocabulary consistent. Skipping any of the three creates a real hole.

- **Domain events are facts, immutable, named in past tense.** Their payloads carry IDs and primitives, never full aggregate references. Their timestamp is for audit and ordering — never for business decisions (clock skew makes wall-time unreliable for that).

- **Documentation is part of the deliverable.** Every public class has class-level Javadoc explaining its role in the architecture (which layer, which context) and its responsibility in domain terms. Every public method has Javadoc with a one-sentence summary in domain language, plus `@param`, `@return` (when non-void), and `@throws` for each exception thrown by intent. Aggregate behavior methods additionally document pre-state, postcondition, and emitted events. Trivial accessors (record components, generated getters) do not need their own Javadoc. Use `/** */` blocks; single-line `//` is not Javadoc. Commented-out code is forbidden — delete it; git keeps history.

## Bounded contexts

The application has four bounded contexts. Each lives in its own package under a single Maven module. Each follows the same layering pattern: domain (POJOs, ports), application (use cases, commands), infrastructure (persistence adapters), and interfaces (REST and/or UI adapters).

### Programming — the Episode aggregate

The source of truth for shows. Episodes are scheduled, filled with content, taken live, and published or canceled.

The Episode aggregate has one inside-aggregate entity, **Abstract** (the show's synopsis), with its own ID. Abstract is replaced — not edited in place — on resubmission.

Invariant table:

| Operation | Pre-state | Postcondition | Domain event |
|---|---|---|---|
| `schedule(number, title, airDate)` | factory (no aggregate exists) | status = SCHEDULED; airDate ≥ today | `EpisodeScheduled` |
| `submitAbstract(text)` | status = SCHEDULED | abstract is set; previous abstract (if any) is replaced | `AbstractSubmitted` |
| `assignPresenter(personId)` | status ∈ {SCHEDULED, LIVE} | presenters contains personId (idempotent) | `PresenterAssigned` only if newly added |
| `assignSpeaker(personId)` | status ∈ {SCHEDULED, LIVE} | speakers contains personId (idempotent) | `SpeakerAssigned` only if newly added |
| `goLive()` | status = SCHEDULED ∧ airDate ≤ today | status = LIVE | `EpisodeWentLive` |
| `publish()` | status = LIVE ∧ abstract ≠ null ∧ \|presenters\| ≥ 1 ∧ \|speakers\| ≥ 1 | status = PUBLISHED | `EpisodePublished` |
| `cancel(reason)` | status = SCHEDULED ∧ reason is non-blank | status = CANCELED | `EpisodeCanceled` |

Value objects:

- **Episode number** — ≥ 1, unique across all episodes (the cross-aggregate rule).
- **Episode title** — non-blank, 1–200 chars.
- **Air date** — a calendar date. Accepts any date including past dates (rehydration from storage requires this); the "must not be in the past" rule is checked in `schedule(...)` against today, not in the value object itself.
- **Abstract text** — non-blank, 100–5000 chars.
- **Episode status** — enumeration: SCHEDULED, LIVE, PUBLISHED, CANCELED.

The Abstract entity has its own ID (separate from the Episode ID) and a submission timestamp; it is replaced wholesale on each `submitAbstract` call.

REST surface: a resource exposing one endpoint per operation under a stable URL prefix (e.g., `POST /api/episodes`, `POST /api/episodes/{id}/abstract`, `POST /api/episodes/{id}/presenters`, `POST /api/episodes/{id}/go-live`, etc.) plus a GET for loading. Map domain exceptions to HTTP status codes per the table below; the mapping is one exception type to one status, implemented in dedicated exception-mapper classes (or equivalent), not in the resource methods.

| Exception family | HTTP status |
|---|---|
| Episode not found | 404 |
| State-machine conflict (e.g., publishing what's not live) | 409 |
| Missing precondition (no abstract, no presenter, no speaker on publish) | 409 |
| Duplicate episode number | 409 |
| Air date in the past (on schedule) or not yet reached (on go-live) | 400 |
| Value-object validation failure (out-of-range, blank, null) | 400 |

### People — the Person aggregate

A single Person aggregate represents anyone who might host or guest on an episode. There are no Presenter or Speaker subtypes — role is owned by Programming, not by People (an Episode tracks which Person IDs are presenters or speakers; a Person doesn't know which Episodes they're on).

Operations: register a new person, rename them, update their bio, update their social links.

Value objects:

- **Person ID** — opaque identifier (UUID-shaped).
- **Person name** — first and last components, each non-blank.
- **Email** — loosely RFC-shaped (presence of `@`, no whitespace, reasonable length); strict RFC validation is not required.
- **Bio** — non-blank, 50–2000 chars.
- **Social links** — a small collection of named links (e.g., site, mastodon, github), each link's URL validated for shape but not reachability.

Domain events: `PersonRegistered`, `PersonRenamed`, `PersonBioUpdated`, `PersonSocialsUpdated`.

REST surface: standard CRUD-style endpoints under a stable URL prefix (e.g., `POST /api/people`, `GET /api/people/{id}`, `PUT /api/people/{id}/name`, and so on). Same exception-to-status conventions as Programming.

### Engagement — Comment and Rating, two aggregates

Comments and ratings are separate aggregates because their invariants are different.

**Comment** is mutable in a narrow window. Operations: submit (auto-approved), edit (only within 5 minutes of original submission; otherwise refuse with a typed "edit window expired" exception). Value objects: a comment ID, an author handle (2–40 chars, `[a-zA-Z0-9_-]`), a comment body (1–2000 chars). Events: `CommentSubmitted`, `CommentEdited`.

**Rating** is immutable after submission. Operations: submit (1–5 stars). Value objects: a rating ID, a star value (1–5). Event: `RatingSubmitted`.

Rating has one cross-aggregate invariant: **one rating per (episodeId, authorHandle)**. A single Rating aggregate cannot check this on its own (it would need to know about other Ratings, which puts them inside its boundary — wrong). Don't create an "EpisodeRatings" super-aggregate to "solve" this — that aggregate would grow unboundedly and serialize every concurrent submission. Instead, apply the three-place rule from the architectural rules above: the application service checks for an existing rating before submitting, a database `UNIQUE (episode_id, author_handle)` constraint catches races, and the persistence adapter catches the constraint-violation exception and re-throws a domain "already rated" exception. The rule is visible in domain code; the DB is a safety net.

REST surface: comment and rating endpoints. The public-catalog context (next section) owns the *HTML* endpoints for submitting comments and ratings via HTMX, but the underlying use cases live in Engagement.

### Catalog — read-only projection + public UI

The public-facing read model. Catalog does not own any domain aggregate. It maintains a single denormalized view per episode that contains everything needed to render the public page:

- episode ID and number
- title and current status
- air date and current abstract (text and submission timestamp)
- embedded presenter summaries (each: person ID + display name)
- embedded speaker summaries (each: person ID + display name)
- comment count, rating count, and average rating

The view is populated by projectors subscribing to domain events from the other three contexts via CDI `@Observes` (or equivalent). One projector per source context:

- A projector for Programming events maintains the episode row itself: status, abstract, air date.
- A projector for People events maintains the embedded presenter/speaker display names (re-resolved when a Person is renamed).
- A projector for Engagement events maintains the rolling counts and average rating.

Projectors must be idempotent on replay; the same event delivered twice must not double-count.

The catalog has one read-only cross-context dependency: a query port for fetching person display names (so the projector can denormalize presenter and speaker summaries into the view). This is the one allowed exception to "no direct cross-context calls" — it's a read, not a command, and it returns DTOs, not aggregates.

UI: server-rendered Qute templates with HTMX for fragment updates. Routes:

| Route | Purpose |
|---|---|
| `GET /` | Landing page — next upcoming episode |
| `GET /episodes` | Paged published episodes, reverse chronological |
| `GET /episodes/{number}` | Episode detail with comments and rating UI |
| `POST /episodes/{number}/comments` | HTMX fragment — append comment row |
| `POST /episodes/{number}/ratings` | HTMX fragment — update average + count |

The two POST endpoints belong to the Catalog context (single source of HTML truth) but delegate the actual writes to Engagement's use cases.

## Testing approach

The test pyramid reflects the architectural layers. Aim for fast tests at the bottom, broader tests as you climb.

- **Pure-domain tests** — JUnit only, no `@QuarkusTest`. Cover every value-object validation rule and every aggregate behavior method including each precondition exception. These run in milliseconds and need no infrastructure.
- **Application-service tests** — Plain JUnit with an in-memory repository implementation (a `HashMap`-backed implementation of the repository port, used only in tests) and a recording event publisher (an implementation of the publisher port that appends events to a list for assertion). These exercise the full service-to-aggregate path without booting Quarkus.
- **Persistence tests** — `@QuarkusTest` with `@TestTransaction` (so each test rolls back). Cover the mapper in both directions, the repository's load and save paths, and the database constraint enforcement (e.g., duplicate episode number, duplicate rating).
- **REST tests** — `@QuarkusTest` with REST Assured. Cover the happy path of each endpoint plus at least one error case per HTTP status.

Quarkus Dev Services brings up PostgreSQL automatically for `@QuarkusTest` runs. No manual datasource configuration is needed in `application.properties`; leave it empty.

## What "done" looks like

The build is complete when:

1. `./mvnw test` is green — pure-domain JUnit tests (no Quarkus boot) plus `@QuarkusTest` integration tests for adapters.
2. `./mvnw quarkus:dev` boots without errors. `curl /q/health/ready` returns 200.
3. The end-to-end happy path works in a browser: schedule an episode → submit an abstract → assign a presenter → assign a speaker → mark it live → publish → comment → rate. The episode appears on `/` (or `/episodes`) at each stage as appropriate.
4. Submitting a second rating for the same (episode, author) returns 409 with a domain "already rated" error, not a `PSQLException` or a 500.
5. Grepping the domain packages for `jakarta.persistence`, `jakarta.ws.rs`, or `io.quarkus.` returns zero hits.
6. Cross-context references in the domain are by ID type, never by direct aggregate reference. Grep helps here: a domain class in one context should not import a domain class from another context.
7. The catalog read model is updated by event subscribers — not by reaching into other contexts' tables.

## Out of scope

- Authentication, authorization, moderation, account management.
- Production deployment manifests beyond what Quarkus generates by default.
- Custom observability (no OpenTelemetry/Prometheus config beyond defaults).
- Cloud-specific persistence configuration. Dev Services handles dev/test.
- Real message brokers. In-process CDI events are sufficient.
- Internationalization, full-text search, pagination beyond the catalog history page.
- Editing or deleting comments after the 5-minute window, changing a submitted rating, change-rating flows.
- Email or notification side-effects.
- Background scheduling (no quartz, no `@Scheduled`).
