# QuarkusInsights — DDD + Hexagonal Architecture + Agentic AI Demo

**Status:** design approved, ready for implementation planning
**Purpose:** consolidate every architectural and presentation decision made during brainstorming so the implementation plan (and any future agent picking up the work) has a single source of truth.

---

## 1. Goals

This demo serves two co-equal purposes. Each stands alone; together they reinforce each other.

### Goal A — A reference architecture for monolithic Quarkus + DDD + Hexagonal

A public reference repository showing how to build a monolithic Quarkus application using Domain-Driven Design and Hexagonal Architecture. The reference application at `quarkusinsightsddd/` on `main` is the canonical artifact — polished, idiomatic, fully tested, designed to be cloned, studied, and copied. Viewers who have no interest in AI should still find this valuable as a "how to do DDD in Quarkus" reference. Most monolithic Quarkus material online is anemic-CRUD-with-MVC; this fills the gap.

### Goal B — Proof that DDD + Hexagonal is a strong fit for agentic development

The same architectural decisions that make a monolith maintainable for humans also make it safe for coding agents. Aggregates, value objects, and bounded contexts protect business invariants by *structure*, not by convention — so when agents (pattern machines, not domain experts) generate plumbing, they can't easily smear business rules into the wrong places. Four CLI coding agents (Claude Code, Codex, Gemini CLI, Copilot CLI) independently build the same application from the same `SPEC.md` in parallel sandboxes. Their outputs are **proof points** — not a competition — that this architecture generalizes across agents regardless of model provider, training data, or CLI. The point isn't who "won"; the point is that any team using any of these CLIs can apply this pattern.

Together: Quarkus optimizes infrastructure, DDD optimizes the business, and the combination produces an application that is *both* maintainable for humans and *agent-friendly* for any team adopting AI assistance.

## 2. Demo format

| Aspect | Value |
|---|---|
| Format | QuarkusInsights episode, ~60 min, no slides |
| Distribution | YouTube live + public GitHub repo |
| Reference app | Pre-built, polished, idiomatic, on `main` |
| Live coding on stream | Naive-CRUD → DDD refactor "tour" + Episode aggregate deep dive |
| Agent execution | Four Docker Sandboxes, YOLO mode, parallel, same `SPEC.md` |
| Validation | Side-by-side examination of all four agent outputs against an architectural checklist (proof points across CLIs, not competition) |

The complete app exists *before* the stream as a reference architecture. Agents do not start from any seed beyond `SPEC.md` and an empty workspace with the Quarkus CLI available.

## 3. Stream arc (60 min)

| Time | Beat | Mode |
|---|---|---|
| 0:00 – 0:03 | Open — thesis + reference app demo | Talk + browser |
| 0:03 – 0:08 | Spec walkthrough + spawn 4 agents | Talk + 4 terminal panes |
| 0:08 – 0:33 | Live-coding tour of DDD (anemic CRUD → DDD constructs, 6 commits) | IDE |
| 0:33 – 0:43 | Episode aggregate deep dive | IDE |
| 0:43 – 0:58 | Agent examination — walk all four outputs against the architectural checklist | Side-by-side editors |
| 0:58 – 1:00 | Wrap — restate thesis with evidence | Talk |

Agents kick off **before** the live-coding tour to maximize their runtime (~50 min). Detailed beat-level notes, the rubric, and the rating-uniqueness teaching note live in `SHOW_NOTES.md`.

## 4. Reference app architecture

### 4.1 Stack & versions

- Quarkus 3.35.2, Java 25 (`maven.compiler.release=25`)
- Hibernate ORM with Panache, **repository pattern** (no active record)
- PostgreSQL 16 via Quarkus Dev Services in dev/test
- Quarkus REST + Jackson for JSON adapters
- Quarkus Qute + HTMX (CDN-loaded) for server-rendered UI
- JUnit 5, REST Assured, plus pure POJO tests for the domain
- Single Maven module at `quarkusinsightsddd/` matching the existing scaffold

### 4.2 Bounded contexts

Four contexts, each a top-level package under `io.arrogantprogrammer.quarkusinsights.*`:

| Package | Responsibility |
|---|---|
| `programming/` | Episodes (with inside-aggregate Abstract), lifecycle, presenter/speaker assignment |
| `people/` | Person aggregate (no Speaker/Presenter sub-types — role is owned by Programming) |
| `engagement/` | Comment and Rating aggregates (separate aggregates — different invariants) |
| `catalog/` | CQRS-flavored read-side projection: landing page, history list, episode detail |
| `shared/` | Cross-context VO IDs (`EpisodeId`, `PersonId`, etc.) and base `DomainEvent` |

### 4.3 Per-context hex layering

```
<context>/
├── domain/              POJO aggregates, VOs, domain events, repository INTERFACE (port)
├── application/         use cases / command handlers, application services
├── infrastructure/
│   └── persistence/     *Entity (JPA), *Mapper, *RepositoryImpl (driven adapter)
└── interfaces/          *Resource (JAX-RS, driving adapter), *Controller (Qute, catalog only)
```

Hex terminology, locked in the spec:
- **Driving ports** = `application/*UseCase`
- **Driven ports** = `domain/*Repository` (no JPA imports)
- **Driving adapters** = `interfaces/*Resource`, `interfaces/*Controller`
- **Driven adapters** = `infrastructure/persistence/*RepositoryImpl`

### 4.4 Persistence pattern (the load-bearing decision)

**Aggregates have zero relationship to persistence.**

- `<context>/domain/` aggregates are pure POJOs. No `jakarta.persistence.*`, `jakarta.ws.rs.*`, `io.quarkus.*` imports.
- `<context>/infrastructure/persistence/` contains `*Entity` JPA classes (`EpisodeEntity`, `PersonEntity`, `CommentEntity`, `RatingEntity`, `PublicEpisodeViewEntity`).
- `*Entity` classes extend **`PanacheEntityBase`** with explicit `@Id UUID id` (not `PanacheEntity`'s auto-generated `Long`).
- `*Mapper` classes translate both directions: `toDomain(E) → A`, `toEntity(A) → E`, `applyTo(A, E)` for updates that preserve `@Version`.
- Repository ports in `domain/` use only domain types in their signatures.
- Repository adapters in `infrastructure/persistence/` use the entity's Panache active-record helpers — that leak is contained to the adapter.

This is the pure POJO + Entity + mapper pattern. Maximum boilerplate, maximum architectural rigor — appropriate for a demo whose entire thesis is domain isolation.

### 4.5 Cross-context integration

In-process domain events via CDI:

- Aggregates record events as part of behavior (`Episode.publish()` returns `EpisodePublished`).
- Application services persist the aggregate, then dispatch via `Event<DomainEvent>`.
- Subscribers in other contexts observe via `@Observes` and react.
- **No direct cross-context service calls.** One exception: a read-only `PeopleQueries` port the catalog uses to enrich projections.

### 4.6 CQRS-flavored read model

The `catalog` context maintains a denormalized `PublicEpisodeView` populated by event projectors:

| Projector | Listens to | Action |
|---|---|---|
| `EpisodeProjector` | `EpisodeScheduled`, `EpisodeWentLive`, `EpisodePublished`, `EpisodeCanceled`, `AbstractSubmitted` | Upsert view, status, abstract |
| `PersonProjector` | `PresenterAssigned`, `SpeakerAssigned`, `PersonRenamed`, `PersonBioUpdated` | Maintain presenter/speaker summaries |
| `EngagementProjector` | `CommentSubmitted`, `RatingSubmitted` | `commentCount`, `averageRating`, `ratingCount` |

Public Qute pages query only `PublicEpisodeView` — they never touch other contexts' tables. **The catalog also owns the HTMX endpoints for posting comments and ratings** (single source of HTML truth); the controllers delegate writes to Engagement use cases.

## 5. Domain models per bounded context

### 5.1 Programming — `Episode` aggregate (root, owns `Abstract` inside-aggregate entity)

| Method | Pre-state | Postcondition | Event |
|---|---|---|---|
| `Episode.schedule(number, title, airDate)` | factory | status = SCHEDULED; airDate > today | `EpisodeScheduled` |
| `submitAbstract(AbstractText)` | status = SCHEDULED | abstract != null | `AbstractSubmitted` |
| `assignPresenter(PersonId)` | status ∈ {SCHEDULED, LIVE} | presenters contains id (idempotent) | `PresenterAssigned` |
| `assignSpeaker(PersonId)` | status ∈ {SCHEDULED, LIVE} | speakers contains id (idempotent) | `SpeakerAssigned` |
| `goLive()` | status = SCHEDULED ∧ airDate ≤ today | status = LIVE | `EpisodeWentLive` |
| `publish()` | status = LIVE ∧ abstract ≠ null ∧ \|presenters\| ≥ 1 ∧ \|speakers\| ≥ 1 | status = PUBLISHED | `EpisodePublished` |
| `cancel(reason)` | status = SCHEDULED | status = CANCELED | `EpisodeCanceled` |

**VOs:** `EpisodeId`, `EpisodeNumber` (≥1), `EpisodeTitle` (1..200), `AirDate`, `AbstractText` (100..5000 chars), `AbstractId`. **Status enum:** `SCHEDULED, LIVE, PUBLISHED, CANCELED`.

### 5.2 People — `Person` aggregate

A **single Person aggregate**, no Speaker/Presenter subtypes — role is a fact owned by the Programming context (Episode tracks `Set<PersonId> presenters` and `Set<PersonId> speakers`). Avoids two parallel hierarchies for one human.

**Behaviors:** `register(name, email, bio)`, `rename(PersonName)`, `updateBio(Bio)`, `updateSocials(SocialLinks)`. **VOs:** `PersonId`, `PersonName(first, last)`, `Bio` (50..2000), `Email` (RFC-ish), `SocialLinks`. **Events:** `PersonRegistered`, `PersonRenamed`, `PersonBioUpdated`, `PersonSocialsUpdated`.

### 5.3 Engagement — two aggregates

#### `Comment`
- `submit(EpisodeId, AuthorHandle, CommentBody)` — auto-approved.
- `edit(CommentBody)` — only within 5 minutes of submission, else `EditWindowExpired`.
- VOs: `CommentId`, `AuthorHandle` (2..40 chars `[a-zA-Z0-9_-]`), `CommentBody` (1..2000 chars).
- Events: `CommentSubmitted`, `CommentEdited`.

#### `Rating`
- Immutable after submission.
- `submit(EpisodeId, AuthorHandle, Stars)` — emits `RatingSubmitted`.
- VOs: `RatingId`, `Stars` (1..5).
- **Uniqueness invariant:** one Rating per `(EpisodeId, AuthorHandle)`. Lives in `SubmitRatingUseCase` + DB `UNIQUE` constraint + adapter exception translation. Detailed rationale in `SHOW_NOTES.md` under the agent shootout section. **Not** a single-aggregate invariant; **not** a DB-only check; **not** an `EpisodeRatings` super-aggregate.

### 5.4 Public Catalog — read model only

`PublicEpisodeView` denormalizes everything needed for landing/history/episode-detail pages. No domain behavior — projectors mutate it in response to events from the other three contexts.

| Route | Template | Purpose |
|---|---|---|
| `GET /` | `landing.html` | Next upcoming episode |
| `GET /episodes` | `history.html` | Paged published episodes, reverse chronological |
| `GET /episodes/{number}` | `episode-detail.html` | Full episode + comments + rating UI |
| `POST /episodes/{number}/comments` | HTMX fragment | Append new comment row |
| `POST /episodes/{number}/ratings` | HTMX fragment | Update average + count |

## 6. `SPEC.md` design

### 6.1 Philosophy

1. **RFC 2119 keywords** in caps (MUST, MUST NOT, MAY) — measurably increases agent compliance over softer language.
2. **Predict the default, then contradict it.** The DO NOT list anticipates the wrong defaults each agent will fall into and forbids them by name.
3. **Prose, not code.** Field listings, type signatures, event payloads, step-by-step behaviors — but no full implementations. Showing implementation makes the demo a transcription test instead of a reasoning test.
4. **Precise about layering, naming, invariant placement; loose about Quarkus mechanics.** Whether an agent uses `@Observes` vs `@Incoming`, `record` vs `class` for VOs, etc. — agents are good at those choices.

### 6.2 Document outline (numbered for back-references)

```
0. Preamble       — what this is, who it's for
1. Goal           — system in one paragraph
2. Stack          — versions, dependencies
3. Architectural rules (DO / DO NOT)   ← load-bearing
4. Naming conventions                  ← locked vocabulary
5. Bounded contexts (5.1–5.4)
6. Cross-cutting (event dispatch, projection wiring, error translation)
7. UI (Qute templates, HTMX, route table)
8. Persistence schema (table list, key constraints)
9. Testing requirements
10. Verification checklist             ← stop condition for agents
11. Out of scope
```

### 6.3 The DO / DO NOT list (§3)

Six to eight rules, each with rationale immediately following:

- **§3.1 Domain isolation** — `<context>/domain/` MUST NOT import `jakarta.persistence.*`, `jakarta.ws.rs.*`, `io.quarkus.*`.
- **§3.2 No Panache inheritance on aggregates** — aggregates MUST NOT extend `PanacheEntity`/`PanacheEntityBase`.
- **§3.3 Validation in VOs** — field-level validation MUST happen in VO constructors.
- **§3.4 Cross-context via events only** — no direct cross-context service calls (catalog's `PeopleQueries` excepted).
- **§3.5 Cross-aggregate references by ID** — never hold a Java reference to another aggregate.
- **§3.6 Rating uniqueness rule** — verbatim three-place enforcement (use case check + DB constraint + adapter translation).

### 6.4 Naming conventions (§4)

Locked vocabulary so all four agent outputs converge on identifiable names for side-by-side reading. Format: aggregate methods are imperative verbs in domain language (`publish`, never `setStatus(PUBLISHED)`); domain events are past-tense participles (`EpisodePublished`); the `*Repository` / `*RepositoryImpl` / `*Entity` / `*Mapper` / `*UseCase` / `*Command` / `*Resource` / `*Controller` / `*Projector` suffixes are mandatory.

### 6.5 Verification block (§10)

Concrete stop condition for YOLO-mode agents. Includes:

- `./mvnw test` green, including pure-domain JUnit tests
- `./mvnw quarkus:dev` boots
- `curl /q/health/ready` returns 200
- `curl /` returns landing page HTML
- End-to-end browser flow: schedule → abstract → presenters/speakers → go-live → publish → comment → rate
- Two ratings from same `(episodeId, author)` → 409 with `AlreadyRated`
- **No `jakarta.persistence`, `jakarta.ws.rs`, or `io.quarkus.*` imports anywhere in any `domain/` package** ← the killer architectural test

### 6.6 Sizing

Estimated 5–6K words / 7–8K tokens. Negligible for any modern coding agent's context window with prompt caching. Single file for portability.

## 7. Repo layout & deliverables

### 7.1 Tree (on `main`)

```
QuarkusInsights/
├── README.md                            ← visitor entry point
├── SPEC.md                              ← contract handed to agents
├── SHOW_NOTES.md                        ← stream timeline + rubric (already exists)
├── LICENSE                              ← Apache 2.0
├── .gitignore                           ← excludes IDEA_PILE.md, agent-os/, CLAUDE.md, ~$*.docx
├── quarkusinsightsddd/                  ← reference application
│   ├── pom.xml, mvnw, src/...
│   └── README.md
├── docs/
│   ├── abstract.md                      ← markdown copy of the talk abstract
│   ├── architecture.md                  ← architecture overview
│   └── architecture-diagram.svg         ← bounded contexts + event flow
└── .github/workflows/
    └── verify-reference.yml             ← `./mvnw test` on push to main
```

### 7.2 Branches

| Branch | Purpose |
|---|---|
| `main` | Reference + docs |
| `live/start` | Naive-CRUD starting point for live refactor |
| `live/end` | Refactor's destination (six commits later) |
| `agent/claude-code` | Claude Code's output (orphan branch) |
| `agent/codex` | Codex's output (orphan branch) |
| `agent/gemini` | Gemini CLI's output (orphan branch) |
| `agent/copilot` | Copilot CLI's output (orphan branch) |

Agent branches are orphans — they didn't share history with `main`, they started from `SPEC.md` and an empty workspace.

### 7.3 Tags

- `v1.0-pre-stream` — frozen `main` the night before
- `v1.0-stream` — final state including all four agent branches as references

### 7.4 CI

Single GitHub Actions workflow on `main` and PRs into it: checkout, JDK 25, `cd quarkusinsightsddd && ./mvnw -B test`. Agent branches get no CI (snapshots, not maintained).

### 7.5 Existing-file disposition

| File | Action |
|---|---|
| `Domain-DrivenDesignAndHexagonalArchitectureWithQuarkusAndAgenticAI.docx` | Convert to `docs/abstract.md`, remove the .docx |
| `~$*.docx` lockfile | Remove from working tree, gitignore the pattern |
| `IDEA_PILE.md` | Gitignore |
| `agent-os/` | Gitignore (per-user Agent OS tooling) |
| `CLAUDE.md` | Already gitignored, stays local |

## 8. Decisions log (canonical record)

| # | Decision | Rationale |
|---|---|---|
| D1 | 60-min YouTube stream + permanent reference repo | Maximum reach + reproducibility; aligns with existing QuarkusInsights cadence |
| D2 | Reference app pre-built; four agents independently build whole app from `SPEC.md` | Reference is the canonical artifact for Goal A. The four agent outputs are proof points for Goal B — that the architecture generalizes across CLIs — not contestants in a shootout |
| D3 | Four bounded contexts (Programming, People, Engagement, Catalog) | Real DDD decomposition with read-side projection; rich enough to grade |
| D4 | Pure POJO domain + `*Entity` + `*Mapper` (not Panache active record, not JPA-on-aggregate) | Strongest hex separation; makes the rubric razor-sharp |
| D5 | `PanacheEntityBase` + explicit `@Id UUID id` (not `PanacheEntity`'s `Long`) | UUIDs match domain VO IDs; `Base` avoids active-record default |
| D6 | Single `Person` aggregate (no `Speaker`/`Presenter`) | Role belongs to Programming context, not to People |
| D7 | Rating uniqueness via use-case check + DB UNIQUE + adapter translation | Makes the rule explicit in domain code while staying race-safe |
| D8 | Catalog owns the HTMX endpoints, delegates writes to Engagement use cases | Single source of HTML truth; engagement context stays pure write-side |
| D9 | Live-coding = naive-CRUD refactor tour + Episode aggregate deep dive | Smell-then-fix narrative; viewers internalize contrast before agent comparison |
| D10 | Agents kicked off **before** live coding | Maximize agent runtime to ~50 min |
| D11 | RFC 2119 prose-not-code spec, anticipating-and-forbidding agent defaults | Spec is a constraint contract, not a tutorial |
| D12 | In-process domain events via CDI `Event<T>` / `@Observes` | No broker complexity; demo is a monolith |
| D13 | Single Maven module matching existing `quarkusinsightsddd/` scaffold | Matches the "monolith" thesis; one project to reason about |
| D14 | Apache 2.0 license | Quarkus-ecosystem default with patent grant |
| D15 | No `COMPARISON.md` post-stream scorecard | Viewers examine for themselves; less repo scaffolding; consistent with proof-points-not-competition framing |
| D16 | Two co-equal goals (A: monolithic DDD reference; B: agentic-friendliness proof) | Reference app stands alone as a Quarkus DDD teaching artifact; agent outputs are proof points across CLIs, not a competition |

## 9. Out of scope (explicit non-goals)

The reference app and agent outputs do **not** include:

- Authentication or user accounts (comments/ratings take a free-form `AuthorHandle`)
- Authorization or moderation flows (comments auto-approve)
- Production deployment manifests (no Helm, no Dockerfile beyond what `./mvnw package -Dnative` generates)
- Observability beyond Quarkus defaults (no custom OpenTelemetry / Prometheus config)
- Cloud-specific persistence config (Dev Services in dev/test; production datasource left unconfigured)
- Multi-language ubiquitous language considerations
- Internationalization
- Pagination beyond the catalog's history page
- Search / filtering beyond the catalog's history page
- Edits, deletes, or change-rating flows (Comments edit within 5 min only; Ratings immutable)
- Email/notification side effects
- Background scheduling (no quartz, no scheduled tasks)

## 10. Open questions / risks

| # | Item | Mitigation |
|---|---|---|
| R1 | Agent runtimes outrun the 50-min window | Pre-stage agent outputs on `agent/*` branches the morning of the stream as fallback |
| R2 | Laptop OOM under four parallel sandboxes | Resources setup procedure in `SHOW_NOTES.md`: warm caches, shared Postgres, Docker resource caps; cloud-VM escalation if 16 GB |
| R3 | Live-coding refactor runs long (>25 min) | Six pre-committed reference commits on `live/end`; can skip step 6 (event emission) and roll into deep dive |
| R4 | One CLI doesn't support YOLO mode (esp. Copilot CLI) | Confirm during pre-flight; if blocked, swap with another CLI (e.g., Cursor agent mode) and update SHOW_NOTES |
| R5 | One or more agents fail multiple architectural checks | Spec is moderately prescriptive on architecture, loose on Quarkus mechanics. Failures are data, not catastrophe: examine where the spec was insufficiently specific and discuss what makes an agent-friendly spec. **Convergence across all four CLIs is the strongest evidence for Goal B**; partial divergence is still useful evidence with discussion. The framing is not "did they pass the test" but "what does this teach us about specifying for agents" |
| R6 | Spec ambiguity discovered during dry run | Dry-run with one CLI 3+ days before stream; iterate spec until output is acceptable |

## 11. Next steps

1. **User reviews this design doc** (you, before transitioning to plan).
2. **Invoke `superpowers:writing-plans`** to produce a step-by-step implementation plan covering: building the reference app context-by-context, writing `SPEC.md`, scaffolding `live/start` and `live/end` branches, drafting `README.md`, configuring CI, creating `docs/abstract.md`, updating `.gitignore`, and dry-running with one CLI.
3. **Implementation in subsequent sessions**, with one or more sub-projects per bounded context plus the spec/repo-scaffolding work.

---

*This design captures architectural decisions only. Stream production details (OBS scenes, camera framing, dry-run schedule, audio setup) live in `SHOW_NOTES.md`.*
