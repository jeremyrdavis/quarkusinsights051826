# Code Review Guide

A practical reviewer's checklist for the QuarkusInsights reference architecture, adapted from Google's [Engineering Practices Code Review Reviewer Guide](https://google.github.io/eng-practices/review/reviewer/) and tailored to this codebase's DDD + Hexagonal Architecture + Quarkus stack.

**Use this document when** reviewing a pull request, dispatching an AI agent to review changes, or performing a periodic audit of the codebase against established standards.

**Companion documents:**
- [`SPEC.md`](SPEC.md) — the architectural contract (also handed to coding agents during the demo)
- [`SHOW_NOTES.md`](SHOW_NOTES.md) — stream timeline + the architectural-checklist rubric used to grade agent outputs
- Root [`CLAUDE.md`](CLAUDE.md) — project-specific instructions

---

## 1. The Standard

> *"The primary purpose of code review is to make sure that the overall code health of the codebase is improving over time."* — Google

A reviewer should approve a CL once it **demonstrably improves code health**, even if imperfect. Perfection is not the bar. But code health must not erode through accumulated small compromises.

**Two competing duties to balance:**
- Don't block developers from making progress with overly restrictive reviews.
- Don't let code quality degrade by waving through compromises.

**Decision authority order:**
1. Style guide rules (absolute on formatting)
2. Engineering principles (objective software design)
3. Personal preference (lowest authority — defer to the author when a valid alternative exists)

When you're not sure, ask: *"Will future readers and maintainers be better served by this change as written?"*

---

## 2. Pre-review checklist (the controller's first pass)

Before line-by-line review, do a one-minute orientation:

- [ ] Read the PR description / commit message. Does the change description match what the diff actually does?
- [ ] Skim the file list. Does the scope match the description, or has unrelated work crept in?
- [ ] Check for surprises: new dependencies in `pom.xml`, deletions of public types, cross-context imports, schema migrations, changes to `CLAUDE.md` / `SPEC.md` / `SHOW_NOTES.md`.
- [ ] Quick mental gut-check: *should this change exist?* If the answer is "no" or "not yet", say so immediately rather than line-reviewing a CL that shouldn't land.

---

## 3. What to look for

Adapted from [Google's checklist](https://google.github.io/eng-practices/review/reviewer/looking-for.html). Each subsection has the general principle followed by **project-specific checks** for this codebase.

### 3.1 Design

> *"Do the interactions of various pieces of code in the CL make sense? Does this change belong in your codebase, or in a library?"*

**General checks:**
- Does the change integrate cleanly with existing systems?
- Is this the right level (domain vs application vs infrastructure vs interfaces)?
- Could it be implemented as a library / extracted utility instead?

**Project-specific checks (DDD + Hexagonal):**
- [ ] **Domain isolation (§3.1 of `SPEC.md`)**: Does any file in `<context>/domain/` import `jakarta.persistence.*`, `jakarta.ws.rs.*`, or `io.quarkus.*`? Run: `grep -rn 'jakarta.persistence\|jakarta.ws.rs\|io.quarkus\.' quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/*/domain/`. Expected: empty.
- [ ] **Aggregate boundaries**: Cross-aggregate references are by ID only (`Set<PersonId>`, never `Set<Person>`).
- [ ] **No cross-context service calls**: Bounded contexts integrate via domain events (CDI `@Observes`). The two documented exceptions are `PersonQueries` (catalog → people read port) and `CommentService`/`RatingService` injection in `EpisodeDetailController` (catalog UI delegating writes per design D8).
- [ ] **Persistence pattern (§4.4)**: Aggregates are pure POJOs in `domain/`. JPA `*Entity` classes live in `infrastructure/persistence/` and extend `PanacheEntityBase` with explicit `@Id UUID id`. A `*Mapper` translates both directions.
- [ ] **Application Service pattern (decision D18)**: New use cases land as methods on the existing `*Service` for the aggregate. Do NOT introduce per-operation `*UseCase` classes — that pattern was rejected in favor of one Application Service per aggregate.
- [ ] **The §3.6 three-place rule for Rating uniqueness**: Any new cross-aggregate uniqueness invariant MUST live in (a) the application service's pre-check, (b) a database UNIQUE constraint, AND (c) the repository adapter's exception translation. Any one of those alone is insufficient.

### 3.2 Functionality

> *"Does this CL do what the developer intended?"*

**General checks:**
- Does the change behave correctly? Edge cases handled?
- Concurrency, race conditions, optimistic locking violations.
- For UI-touching CLs: pull the change, run it, and click through the user flow.

**Project-specific checks:**
- [ ] **Aggregate invariants enforced inside the aggregate**, not in the service or REST resource. (E.g., `Episode.publish()` checks `abstract != null && presenters >= 1 && speakers >= 1`, not `EpisodeService.publish`.)
- [ ] **`@Version` on every entity that supports concurrent writes**, including read-side views written by multiple projectors.
- [ ] **Domain events recorded inside aggregate behaviors**, drained by application services after `repository.save(...)`.
- [ ] **`@Transactional` boundary at the application service**, not on repository methods or aggregates.

### 3.3 Complexity

> *"Could the code be made simpler? Would another developer be able to easily understand and use this code when they come across it in the future?"*

- [ ] Is this **over-engineered for speculative future needs**? Solve the present problem.
- [ ] Method length: anything over ~30 lines warrants a question — can it be split?
- [ ] Conditional depth: nested `if/else` beyond two levels is a refactor flag.
- [ ] Are there premature abstractions? The reference architecture's intentional duplication (e.g., `loadOrThrow` repeated across services) is acceptable when fewer than 4 instances exist; revisit on the 4th occurrence.

### 3.4 Tests

> *"Ask for unit, integration, or end-to-end tests as appropriate for the change."*

**General checks:**
- Tests cover the new behavior, not just the happy path.
- Tests fail when the behavior they verify breaks. (Tautological tests are worse than none.)
- Test names describe what's being verified.

**Project-specific checks:**
- [ ] **Pure-domain tests are pure JUnit**, no `@QuarkusTest`. Aggregate invariants tested with `@Nested` groups per behavior method.
- [ ] **`*ResourceTest.java` has a `*IT.java` companion** — `class FooIT extends FooTest {}` with `@QuarkusIntegrationTest`. CLAUDE.md mandates this pair. *Currently absent for many resource tests — flag any new resource test that ships without an IT.*
- [ ] **`@QuarkusTest @TestTransaction`** for repository implementation tests so each test rolls back. Persistent test mutations pollute the shared Postgres.
- [ ] **Service tests use stub repositories** (`InMemory*Repository`) and the shared `RecordingDomainEventPublisher` — no Quarkus startup for application-layer tests.
- [ ] **Cross-test isolation**: tests sharing the Postgres at the HTTP boundary use `AtomicInteger` counters or unique-per-test discriminators (e.g., authorHandle suffixes). `synchronized` instance methods on static counters are insufficient.

### 3.5 Naming

> *"Did the developer pick good names for everything?"*

- [ ] Aggregate behavior methods are imperative verbs in domain language: `publish`, `submitAbstract`, `assignPresenter` — not `setStatus(PUBLISHED)`.
- [ ] Domain events are past-tense participles: `EpisodePublished`, `RatingSubmitted`.
- [ ] Mandatory suffixes (per `SPEC.md` §4): `*Repository` (port) / `*RepositoryImpl` (adapter) / `*Entity` (JPA) / `*Mapper` / `*Service` (Application Service) / `*Command` / `*Resource` / `*Controller` (Qute UI) / `*Projector`.
- [ ] Test methods describe behavior: `publishRequiresAtLeastOnePresenter`, not `testPublish1`.
- [ ] Local variables use camelCase, not snake_case (Java convention).

### 3.6 Comments and Javadoc

> *"Comments should explain why some code exists, and should not be explaining what some code is doing."*

**Project-specific (per `SPEC.md` §4.7 — comprehensive Javadoc is part of the deliverable):**
- [ ] **Every public class** has class-level Javadoc explaining (a) its role in the architecture (which layer, which bounded context), (b) its responsibility in domain terms, and (c) any invariants it protects.
- [ ] **Every public method** has Javadoc with `@param`, `@return`, `@throws` as applicable.
- [ ] **Records** have `@param <component>` tags at the **class level** (not just on the compact constructor). Without them, `./mvnw javadoc:javadoc` reports `Missing a Javadoc @param` warnings.
- [ ] **Aggregate behavior methods** document pre-state required, postcondition guaranteed, and event(s) emitted.
- [ ] **No restate-the-obvious comments** (e.g., `// returns the id` over a `getId()` method).
- [ ] **No `//` single-line comments** for class or method documentation — Javadoc `/** */` is required.
- [ ] **No commented-out code blocks** — delete; git keeps history.
- [ ] First sentence of Javadoc is terse and self-contained — IDEs and Javadoc tools use it as the summary.

### 3.7 Style and consistency

> *"Use the STYLE label so the developer knows it's a comment about style."*

- Style guide is the absolute authority. Mandatory style fixes are not optional.
- Mark non-mandatory polish with `Nit:`.
- For competing styles in the codebase: the change should be consistent with the surrounding code, not introduce a third pattern.

**Project conventions:**
- Records over classes for VOs and events; classes for aggregates with mutable lifecycle.
- Constructor injection (not field injection) — `@Inject` on a constructor.
- `IllegalArgumentException` (not `Objects.requireNonNull`) for null/range validation in VO constructors so the exception type matches the VO contract.

### 3.8 Documentation

> *"If a CL changes how users build, test, interact with, or release code, check to see that it updates associated documentation."*

- [ ] Changes to `pom.xml` dependencies → update README if affecting setup
- [ ] New REST endpoints → update relevant `*Resource` Javadoc endpoint table
- [ ] New aggregate behaviors → update `SPEC.md` §5.x if it's described there
- [ ] Architectural shifts → update `docs/superpowers/specs/...-design.md` decisions log

### 3.9 Every line

> *"Look at every line of code that you have been assigned to review."*

For data files, generated code, large test data: it's OK to scan rather than read line-by-line. Be explicit if you do this.

For domain code, Application Services, and adapters in this project: read every line. The architecture is the lesson.

### 3.10 Context

> *"It is sometimes helpful to look at the CL in broad context."*

Open the file in the editor, not just the diff. Does the change make the file's overall design better or worse? A line-level "improvement" can degrade the file's coherence.

### 3.11 Good things

> *"If you see something nice in the CL, tell the developer, especially when they addressed one of your comments in a great way."*

Code review is not just for catching problems. Naming a particularly clean refactor or a clever test fixture builds the right standards on the team.

---

## 4. How to navigate a review

From [Google's Navigating a CL guide](https://google.github.io/eng-practices/review/reviewer/navigate.html):

1. **Take a broad view.** Read the description. Make sense of the change at a high level. If you don't understand WHY the change exists, push back before reading the diff.
2. **Examine the main parts.** The largest / most-significant file usually carries the design decision. Review it first. If you find a design flaw here, raise it immediately — don't read the rest yet.
3. **Look at the rest in a logical order.** Often: tests first (they describe intended behavior), then implementation. Adapter / boilerplate last.

**Project-specific reviewer order:**
- For domain-layer changes: `*Test.java` (the spec) → aggregate / VO source → events → repository port
- For application-layer changes: `*ServiceTest.java` → `*Service.java` → commands
- For adapter changes: integration test → REST or repository implementation → mapper / DTO

---

## 5. Writing review comments

Adapted from [Google's How to Write Comments guide](https://google.github.io/eng-practices/review/reviewer/comments.html).

### Be courteous

> *"Be polite and respectful while also being very clear and helpful to the developer."*

- Comment on the **code**, not the developer. ❌ *"Why did you use threads here?"* ✅ *"The concurrency model adds complexity without measurable benefit."*
- Avoid loaded language ("obviously," "of course," "you should know").

### Explain reasoning

Don't just say "change this." Say "change this because…". The developer learns; the next reader of the file benefits.

### Prefix tags

Use prefix tags to signal severity so the author knows what's blocking:

- `Nit:` — non-blocking style or polish
- `Optional:` / `Consider:` — suggestion the author can take or leave
- `FYI:` — informational, no action needed
- `Question:` — clarifying ask, not yet a request to change
- *(no prefix)* — should be addressed before merge

### Direction vs guidance

Two valid styles, depending on context:
- **Pointing out a problem** ("the unique constraint is missing on this column") teaches more.
- **Direct instruction** ("add `@UniqueConstraint(name=\"uk_…\", columnNames={…})` to the table annotation") moves faster.

Use direct instruction when the fix is mechanical and the convention is established. Use the problem-pointing style when there are multiple valid solutions and the developer should choose.

### Don't accept lengthy explanations in review

> *"If a developer can't explain their code in clear terms, the explanation should go in the code itself, not the review tool."*

If the only way the change makes sense is with a paragraph in the review thread, that paragraph belongs in the source as a comment.

---

## 6. Speed of reviews

From [Google's Speed of Code Reviews guide](https://google.github.io/eng-practices/review/reviewer/speed.html):

- **Respond within one business day** to a review request. Multiple cycles in a day is the goal for non-urgent CLs.
- **Don't context-switch mid-task** to do reviews. Use natural breaks (between tasks, after a meeting, before lunch).
- **For large CLs**: ask the author to split into smaller sequential changes. Review of an enormous diff is rarely thorough.
- **LGTM with comments** is fine when you trust the author to address the comments and they're not load-bearing.
- **Cross-time-zone**: aim to complete reviews before the author's next workday begins.

---

## 7. Handling pushback

From [Google's Handling Pushback guide](https://google.github.io/eng-practices/review/reviewer/pushback.html):

- **Consider that the developer might be right.** They're often closer to the code than you are.
- **Stand firm when you're confident.** Improving code health happens in small steps; one accepted compromise becomes the norm for the next.
- **Don't accept "I'll clean it up later."** Experience shows it rarely happens. Require fixes before approval unless the work has explicit follow-up tracking.
- **Stay calm.** Worry about developer frustration is usually in the reviewer's mind. Tone matters more than insistence.
- **Escalate when stuck.** If consensus is impossible, escalate to a tech lead rather than letting the CL stall.

---

## 8. Project-specific reviewer rubric

This rubric is the same one published in `SHOW_NOTES.md` for grading agent outputs during the demo. Use it as a quick-pass scorecard for any change that touches the four bounded contexts.

| # | Commitment | Honored looks like | Diverged looks like |
|---|---|---|---|
| 1 | **Domain purity** | `domain/` has zero imports of `jakarta.persistence`, `jakarta.ws.rs`, `io.quarkus.*` | Any of those imports inside `domain/` |
| 2 | **Aggregate invariants** | Behavior methods enforce all preconditions inside the aggregate; throw domain exceptions | Any precondition lives in the application service, REST resource, or DB constraint alone |
| 3 | **Persistence pattern** | `*Entity` in `infrastructure/persistence/`, mapper translates, `PanacheEntityBase` with `UUID id`, `@Version` for concurrent writers | Aggregate extends `PanacheEntity*`; or aggregate carries `@Entity`; or no `@Version` on contended writes |
| 4 | **REST thinness** | Resources delegate to Application Services; no validation in controllers (VOs handle it); no repository injection in resources; no inline `WebApplicationException` throws | Controllers contain `if`-checks for business rules; resources inject repositories; `throw new NotFoundException(...)` in resource bodies |
| 5 | **Event-driven integration** | Catalog projections update via `@Observes` on domain events; cross-context dependencies via documented read ports only | Catalog reads directly from Programming/People/Engagement tables, or calls their services for reads |
| 6 | **Cross-context references by ID** | Episode holds `Set<PersonId>`, never `Set<Person>` | Direct entity references across contexts |
| 7 | **Cross-aggregate uniqueness (§3.6 rule)** | Use case checks first + DB UNIQUE constraint + adapter translates `ConstraintViolationException` to the domain exception | DB-only enforcement, or check in REST, or `*Aggregate*` super-aggregate |
| 8 | **Test pair pattern** | Every `*ResourceTest.java` has a `*IT.java` companion extending it under `@QuarkusIntegrationTest` | `@QuarkusTest` resource tests with no IT companion |
| 9 | **Test isolation** | Repository tests use `@TestTransaction`; HTTP-boundary tests use `AtomicInteger` for unique discriminators | Manual `deleteEntity` cleanup; `Math.random()`; `static int` with instance `synchronized` |
| 10 | **Documentation per §4.7** | Every public class + method has Javadoc; records have `@param` at class level | Missing Javadoc; `// inline` comments; commented-out code |

---

## 9. Reviewer tools for this codebase

Quick commands for spot-checks:

```bash
# Domain isolation killer test (must return empty for every context)
grep -rn 'jakarta.persistence\|jakarta.ws.rs\|io.quarkus\.' \
  quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/*/domain/

# Find @QuarkusTest classes without an IT companion
for f in $(find quarkusinsightsddd/src/test -name '*ResourceTest.java' -o -name '*ControllerTest.java'); do
  it="${f%Test.java}IT.java"
  [ -f "$it" ] || echo "MISSING IT: $f"
done

# Verify Javadoc compiles cleanly on a target file
cd quarkusinsightsddd && ./mvnw -B javadoc:javadoc 2>&1 | grep -iE 'warn|error|missing'

# Find @Observes methods missing @Transactional (projector hazard)
grep -rB1 '@Observes' quarkusinsightsddd/src/main/java/ \
  | grep -A1 '@Transactional' | grep -B1 '@Observes' || echo "manually inspect projector files"

# Find resources injecting repositories (rule 4 violation)
grep -rn '@Inject.*Repository' quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/*/interfaces/

# Find resource methods returning DTOs directly (rule 4 violation — should return Response)
grep -rn 'public [A-Z][a-zA-Z]*Response \|public List<[A-Z][a-zA-Z]*Response>' \
  quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/*/interfaces/
```

---

## 10. When to invoke this guide

| Situation | Use |
|---|---|
| Reviewing a colleague's PR | The full guide; emphasize sections 3, 5, 7 |
| Self-review before pushing | Sections 3, 8, 9 (the spot-check commands) |
| Periodic codebase audit | Sections 8, 9 — run the bash spot-checks |
| Dispatching an AI agent to review | Reference this document by URL in the dispatch prompt; ask the agent to apply sections 3 + 8 |
| Live-stream demo prep | Use section 8 (the rubric) as the on-screen scorecard |

---

*Sources: [Google Engineering Practices — Code Review Reviewer Guide](https://google.github.io/eng-practices/review/reviewer/) (Apache 2.0 licensed). Project-specific checks are derived from `SPEC.md`, the design doc, `CLAUDE.md`, and the architectural decisions log.*
