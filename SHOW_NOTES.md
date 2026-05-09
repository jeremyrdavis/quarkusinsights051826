# QuarkusInsights — DDD + Hexagonal Architecture + Agentic AI

Stream notes for the QuarkusInsights episode. Working draft.

**Two co-equal goals for the episode:**

1. **Reference architecture** — show how to build a maintainable monolithic Quarkus app using DDD and Hexagonal Architecture. The reference app at `quarkusinsightsddd/` stands on its own as a "how to do DDD in Quarkus" teaching artifact, valuable to viewers who care nothing about AI. Most monolithic Quarkus material online is anemic-CRUD-with-MVC; this fills the gap.
2. **Agentic-friendliness proof** — show that the same architecture is a strong fit for AI-assisted development. The four agent outputs are **proof points across CLIs**, not a competition: any team using any of these tools can apply this pattern. Strong DDD boundaries protect business invariants by *structure*, not by convention, so coding agents (which are pattern machines, not domain experts) can generate plumbing without smearing business rules into the wrong places.

---

## At a glance (60-min target)

| Time | Beat | Mode |
|---|---|---|
| 0:00 – 0:03 | Open — thesis + reference app demo | Talk + browser |
| 0:03 – 0:08 | Spec walkthrough + spawn 4 agents in Docker Sandboxes | Talk + 4 terminal panes |
| 0:08 – 0:33 | Live-coding tour of DDD (anemic CRUD → DDD constructs) | IDE |
| 0:33 – 0:43 | Episode aggregate deep dive | IDE |
| 0:43 – 0:58 | Agent examination — walk all four outputs against the architectural checklist | Side-by-side editors |
| 0:58 – 1:00 | Wrap — restate both goals with evidence | Talk |

---

## Resources setup

Running four Docker Sandboxes in parallel hits the laptop hard during build cycles — each agent's `mvn test` and `quarkus:dev` invocations spawn JVMs and (by default) Dev Services Postgres containers. Worst-case parallel build can reach ~12–16 GB RAM and saturate 8+ cores. The spec itself (~7–8K tokens to the LLM) is irrelevant to laptop load; what matters is what the agents *do* on disk and in JVMs. Do the prep below the day before the stream, in this order.

### 1. Warm the Maven caches (biggest win)

Cold dependency downloads are the dominant first-five-minutes spike. Eliminate them by running one full build per sandbox the day before:

```bash
# In each Docker Sandbox, after cloning the seed project skeleton:
cd /workspace/quarkusinsightsddd
./mvnw -DskipTests dependency:go-offline   # pulls all deps once
./mvnw test                                 # exercises the test path too
```

Verify `~/.m2/repository` is populated in each sandbox image. If your sandboxes are ephemeral (recreated for the show), bake the warmed `~/.m2` into the base image so every fresh sandbox starts hot.

### 2. Share one Postgres across all four sandboxes

Default Quarkus behavior is to let Dev Services start a Postgres testcontainer per app — that's four Postgres containers totaling ~1 GB RAM, plus four container-start delays. Replace with a single shared instance:

```bash
# On the host (or anywhere all four sandboxes can reach):
docker run -d --name qi-shared-pg \
  -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo \
  -p 5432:5432 \
  postgres:16-alpine

# Create one DB per sandbox to keep schemas isolated:
for db in cc codex gemini copilot; do
  docker exec qi-shared-pg createdb -U demo "qi_$db"
done
```

Then **inject env vars at sandbox launch** (do not put this in `SPEC.md` — agents start from scratch and shouldn't see infra hints):

```bash
# Per sandbox, replacing <agent> with cc / codex / gemini / copilot:
docker run ... \
  -e QUARKUS_DATASOURCE_DB_KIND=postgresql \
  -e QUARKUS_DATASOURCE_USERNAME=demo \
  -e QUARKUS_DATASOURCE_PASSWORD=demo \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/qi_<agent> \
  ...
```

When env vars are present, Quarkus skips Dev Services and uses the shared DB. Each agent's writes land in their own database, so cross-contamination is impossible.

### 3. Cap Docker Desktop's resource pool

On macOS, **Docker Desktop → Settings → Resources**:

- Memory: 12 GB (leave 4+ GB for host OS, OBS, browser)
- CPUs: 6 (leave 2+ for streaming and IDE)
- Swap: 2 GB

This deliberately limits how greedy the four sandboxes can collectively be. Better that builds run a bit slower than that OBS stutters or the laptop swaps.

### 4. Stagger agent kickoffs by ~30 seconds

When you spawn the four sandboxes during the show (0:03–0:08 segment), don't fire all four simultaneously. Hit return on one, count to thirty, hit return on the next. Spreads the initial dependency-resolution and JVM-startup spikes so they don't pile up.

### 5. Cloud-VM workaround (if any of the above is in doubt)

If the laptop is < 32 GB RAM, or if a dry run shows the laptop swapping or OBS stuttering during parallel builds, move the sandboxes off the laptop entirely:

- Provision one beefy VM (e.g., 32 GB / 8 vCPU on AWS / GCP / Hetzner / DigitalOcean)
- Install Docker; clone the same sandbox base image
- Run all four sandboxes on the VM
- From the laptop: `ssh` (or `code-tunnel` / `mosh` / VS Code Remote-SSH) into each sandbox during the examination

The laptop then does nothing but stream and display. Spec size, build cycles, and JVM memory all stop mattering for the local machine.

This is the safest play if the dry run reveals any margin doubt — better to know you have headroom than to discover at minute 47 that a sandbox is OOM-killed.

### 6. Pre-flight check (morning of the stream)

Quick verification that everything still works:

```bash
# Each sandbox should pass this in under 2 minutes (with warm cache):
time ./mvnw test

# Then start all four sandboxes in parallel and run mvnw test in each at once.
# Watch:
#   - macOS Activity Monitor: peak RAM stays under your Docker cap
#   - All four mvnw tests succeed
#   - OBS preview stays smooth throughout
```

If any of those fail, escalate to the cloud-VM workaround.

---

## Pre-show checklist

- [ ] Reference app running locally on `http://localhost:8080` with seed data loaded (a few episodes, presenters, sample comments/ratings)
- [ ] Four Docker Sandboxes pre-provisioned and warm — Claude Code, Codex, Gemini CLI, Copilot CLI — each ready to clone-and-run
- [ ] `SPEC.md` published at the public GitHub URL; `wget`/`curl` of the URL tested from inside each sandbox
- [ ] Pre-staged agent outputs on branches `agent/claude-code`, `agent/codex`, `agent/gemini`, `agent/copilot` as a fallback if a sandbox fails mid-stream
- [ ] IDE workspace pre-loaded with the naive-CRUD starting point on a `live/start` branch, ready for the refactor demo
- [ ] Stopwatch / OBS timer visible in your view but not on stream
- [ ] Browser tab pinned: reference app's landing page + a separate tab for episode detail with a comment already submitted
- [ ] Resources setup steps 1–3 completed; pre-flight check (step 6) passed this morning

---

## 0:00 – 0:03 — Open

- Hit the reference app live: landing page → click upcoming episode → submit a comment → submit a 5-star rating. Show the average update.
- One-liner: *"This is what we're going to ship by the end of the hour. We're going to do two things at once. First, build a reference architecture for monolithic Quarkus apps using Domain-Driven Design and Hexagonal Architecture — the kind of monolith you'll still want to maintain three years from now. Second, prove that this style of architecture is a strong fit for AI-assisted development by having four different coding agents — Claude Code, Codex, Gemini CLI, and Copilot CLI — independently build the same app from the same spec, in parallel. We'll see whether the architectural commitments hold up across all four CLIs."*
- State both goals once, clean. Don't over-explain — the rest of the show is the explanation.

---

## 0:03 – 0:08 — Spec walkthrough + agent kickoff

**Goal:** get all four agents running before the live-coding starts so they have ~50 minutes of runtime.

- Open `SPEC.md` on screen. Don't read it line by line — highlight the sections:
  1. Bounded contexts (4) and the rule that each is a top-level package
  2. Persistence: pure POJO domain + `*Entity` JPA classes + mappers (call this out — *"this is the line agents will be most tempted to cross"*)
  3. The Rating uniqueness invariant box (call attention — *"this is the trickiest one"*)
  4. The "DO NOT" list at the bottom — `@Entity` in `domain/`, business logic in REST, cross-context service calls
- Switch to a 4-pane terminal layout. In each pane, paste the prepared kickoff command. Spawn Claude Code, Codex, Gemini CLI, Copilot CLI sequentially — each picks up the spec URL.
- Confirm all four are progressing (look for "Cloning…" / "Reading SPEC.md"). Then walk away from the terminals — they'll run in the background until the examination segment.

**Don't get sucked into watching them.** Snap back to the IDE.

---

## 0:08 – 0:33 — Live-coding tour of DDD

**Goal:** teach DDD by refactoring an anemic CRUD endpoint into rich domain constructs, on its own merits. This serves Goal A directly — it's the reference architecture lesson, valuable to viewers who care nothing about agents — and gives viewers a mental template for what the agents are likely to produce. The lesson stands without the agentic frame.

Start branch: `live/start` — naive CRUD endpoint for "submit abstract."

```java
// Starting state — all in one file:
@Path("/abstracts") @ApplicationScoped
public class AbstractResource {
  @Inject EntityManager em;
  @POST @Transactional
  public Response submit(SubmitDto dto) {
    if (dto.text == null || dto.text.length() < 100) return badRequest();
    if (dto.text.length() > 5000) return badRequest();
    var entity = new AbstractJpa();
    entity.text = dto.text;
    entity.episodeId = dto.episodeId;
    em.persist(entity);
    return Response.ok(entity.id).build();
  }
}
```

**Refactor steps (one git commit each, named for the construct introduced):**

1. **Extract Command** — `SubmitAbstractCommand` record. *"This is the input. It has no behavior. It's the language the outside world speaks to us in."*
2. **Extract Value Object: `AbstractText`** — moves the length check into the constructor. *"You can't create an `AbstractText` that's invalid. The type itself is the rule."* Show that the resource's validation lines just disappeared.
3. **Extract Application Service: `SubmitAbstractUseCase`** — pulls the orchestration out of the resource. The resource is now four lines.
4. **Introduce Aggregate Root: `Episode`** — the `Abstract` belongs to an `Episode`. The use case loads the Episode, calls `episode.submitAbstract(text)`, saves the Episode. The behavior moved off the JPA entity and onto a domain-language method.
5. **Introduce Repository port** — `EpisodeRepository` interface in `domain/`. Show that the use case depends on the interface, not on Panache. The Panache implementation lives in `infrastructure/persistence/`.
6. **Emit Domain Event** — `Episode.submitAbstract(...)` records `AbstractSubmitted`; the use case publishes via `Event<DomainEvent>` after `save`. Tease that the catalog projector listens to this — *"we'll see why in a moment."*

**Talking points to hit:**
- After step 2: *"This is the most underappreciated move in DDD. We didn't add a layer; we made invalid states unrepresentable."*
- After step 4: *"This is what we mean by 'rich domain.' The behavior is on the noun, not on a service named `EpisodeService` that takes the noun as a parameter."*
- After step 5: *"Now ask yourself — could the domain even compile if I deleted Quarkus from the project? Yes. That's hex architecture in one sentence."*

**Time discipline:** this section is 25 minutes. If you're at step 4 by 0:23, skip the event emission demo (step 6) and roll it into the deep dive.

---

## 0:33 – 0:43 — Episode aggregate deep dive

**Goal:** the heart of DDD — show what aggregates *protect*.

- Open `programming/domain/Episode.java` from the reference app.
- Walk through the lifecycle: `SCHEDULED → LIVE → PUBLISHED`. Point at the `EpisodeStatus` enum.
- Walk through `publish()`:
  ```java
  public List<DomainEvent> publish() {
    if (status != LIVE) throw new IllegalEpisodeTransition(...);
    if (abstract_ == null) throw new MissingAbstract();
    if (presenters.isEmpty()) throw new MissingPresenter();
    if (speakers.isEmpty()) throw new MissingSpeaker();
    this.status = PUBLISHED;
    return List.of(new EpisodePublished(id, number, Instant.now()));
  }
  ```
- *"Three things to notice: (1) every transition that could be invalid throws — you cannot get to a published episode through any path other than this method. (2) The method is named in domain language — `publish`, not `setStatus(PUBLISHED)`. (3) It returns events, not void — the aggregate tells the world what just happened."*
- Open `programming/domain/EpisodeTest.java`. Run it. Pure JUnit, zero `@QuarkusTest`, zero startup time. *"This is what 'isolating the domain' actually buys you."*
- Demonstrate one failing test: try to publish without an abstract. Show the assertion. *"This is the invariant. If an agent moves this check into a REST resource, the test still passes against a stubbed Episode — and the rule has silently moved out of the domain."*

---

## 0:43 – 0:58 — Agent examination

**Goal:** walk through all four agent outputs side-by-side against the architectural checklist. The framing is *proof points across CLIs*, not a competition. Convergence on the reference architecture across four model providers, four training datasets, and four CLIs is the headline evidence for Goal B — it means a team using any of these tools can apply this pattern. Variation between agents is data, not failure: it shows where the spec might benefit from more specificity. ~3 minutes per agent + a closing synthesis.

### The architectural checklist (post on screen)

Seven architectural commitments to look for in each agent's output. The framing is "would a Quarkus DDD team be happy to maintain this?" — not "did it pass a test."

| # | Commitment | Honored looks like | Diverged looks like |
|---|---|---|---|
| 1 | **Domain purity** | `domain/` has zero imports of `jakarta.persistence`, `jakarta.ws.rs`, `io.quarkus.*` | Any of those imports inside `domain/` |
| 2 | **Aggregate invariants** | `Episode.publish()` enforces all four preconditions | Any precondition lives in the use case, REST resource, or DB constraint |
| 3 | **Persistence pattern** | `*Entity` classes in `infrastructure/persistence/`, mapper translates, `PanacheEntityBase` with `UUID id` | Aggregate extends `PanacheEntity*`; or aggregate has `@Entity` |
| 4 | **REST thinness** | Resources delegate to use cases; no validation in controllers (VOs handle it) | Controllers contain `if`-checks for business rules |
| 5 | **Event-driven integration** | Catalog projection updates via `@Observes` on domain events | Catalog reads directly from Programming/People/Engagement tables |
| 6 | **Cross-context references by ID** | Episode holds `Set<PersonId>`, never `Set<Person>` | Direct entity references across contexts |
| 7 | **Rating uniqueness handling** | Use case checks + DB constraint + adapter exception translation | DB-only, or check in REST, or `EpisodeRatings` super-aggregate |

### Examination order

1. **Claude Code** — open in left pane. Walk the checklist top-to-bottom. Note where each commitment was honored or where the agent diverged.
2. **Codex** — middle-left pane. Same checklist. Call out divergences in domain language (e.g., "this one made `Episode` extend `PanacheEntity` — the persistence type leaked into the aggregate").
3. **Gemini CLI** — middle-right pane. Same.
4. **Copilot CLI** — right pane. Same.
5. **Synthesis** (last 3 min): which architectural commitments held up across all four CLIs? Where did agents differ in adapter-level choices? *"Convergence across four different agents on a strict architectural style is the headline result. It means a team using any of these CLIs can apply this pattern with confidence — the architecture, not the agent, is doing the work. Where agents diverged, the spec could be more specific without changing what we're building."*

### Teaching note: the rating uniqueness invariant

This is the single most informative examination moment. Take the time on it (3-4 min). The point isn't whether each agent "passed" — it's *where each one put the rule*, because the where reveals each CLI's defaults.

There are four candidate locations for the rule "one rating per (EpisodeId, AuthorHandle)":

**1. Inside the `Rating` aggregate itself.** Doesn't work. A single `Rating`'s state is `{episodeId, author, stars, submittedAt}`. Whether some *other* Rating exists for the same `(episodeId, author)` is a fact about the *collection*, not about any one Rating. To check it from inside `Rating`'s constructor, the Rating would have to load a list of siblings — which means the aggregate boundary now contains "all ratings related to this episode."

**2. A larger aggregate: `EpisodeRatings` containing all ratings for one episode.** Technically works, practically wrong. Make `EpisodeRatings` the root holding `Set<Rating>`; `EpisodeRatings.submit(...)` checks the set then adds. The invariant becomes a true aggregate invariant — but every submission loads thousands of ratings into memory, every submission contends on the same aggregate version (a popular episode's rating endpoint serializes itself), and the aggregate has unbounded growth. This is the canonical example Vaughn Vernon uses in *Effective Aggregate Design* of "modeling the world" — the textbook *don't*.

**3. In the use case (application service), via a repository query.** What the spec prescribes:

```java
class SubmitRatingUseCase {
  void handle(SubmitRatingCommand cmd) {
    ratingRepository.findByEpisodeAndAuthor(cmd.episodeId(), cmd.author())
      .ifPresent(existing -> { throw new AlreadyRated(existing.id()); });
    var rating = Rating.submit(cmd.episodeId(), cmd.author(), cmd.stars());
    ratingRepository.save(rating);
    eventPublisher.publish(rating.events());
  }
}
```

The rule is **explicit in domain-near code**, **expressed in domain language** (`AlreadyRated` is a domain exception, not a `PSQLException`), and **discoverable** — a reader scanning use cases sees the invariant immediately. This is the right home in DDD when the rule spans aggregates: an *application service* coordinates between aggregates and the repository.

**4. DB unique constraint only.** The agent trap. `UNIQUE (episode_id, author_handle)`. Submission throws `ConstraintViolationException`; the REST resource catches and returns 409. Done. But:
- The rule is invisible in the domain code. A reader has to read DDL to know it exists.
- The use case has to catch a *persistence* exception and translate it into a *domain* meaning. That's hex's directionality reversed: infrastructure dictating domain semantics.
- A future change ("allow change-rating") has nothing in the domain to update — the rule lives in the DB and now requires a schema migration coordinated with code.

**Why the spec prescribes #3 *plus* a DB constraint as belt-and-suspenders:** option 3 alone has a race window — between `findByEpisodeAndAuthor` (no row) and `save` (insert), a parallel transaction can submit. Both pass the check, both insert. The robust pattern:

- **Use case checks first** for *correctness of intent* and clean error reporting in the 99.99% case. The user gets `AlreadyRated`, a domain exception, with the existing rating's ID.
- **DB has a `UNIQUE (episode_id, author_handle)` constraint** for *correctness of state* in the rare race. If the constraint fires, the repository adapter catches `ConstraintViolationException` and **re-throws the same `AlreadyRated` domain exception**. The translation happens in the adapter (where infrastructure errors are allowed); the use case never sees the persistence exception.

The DB constraint is a safety net, not the rule. The rule is in the use case.

**What to look for in each agent's output (the three-question check):**
1. Did `SubmitRatingUseCase` check `findByEpisodeAndAuthor`? *(Required.)*
2. Did the schema declare `UNIQUE (episode_id, author_handle)`? *(Required.)*
3. Did the adapter translate `ConstraintViolationException → AlreadyRated`? *(Required.)*

Common divergences to call out by name when you see them:
- **DB-only** — *"This agent went straight to a constraint. The rule is invisible in the domain. If I delete the DDL, the application has no idea this rule ever existed."*
- **Check in the REST resource** — *"Same logic, wrong layer. Now the rule lives in JAX-RS code, which means anyone writing a different adapter — say, a CLI or a message consumer — has to remember to re-implement it."*
- **`EpisodeRatings` super-aggregate** — *"Pure DDD on paper, but watch what happens at scale: every rating submission for a popular episode loads every prior rating into memory."*

---

## 0:58 – 1:00 — Wrap

- Restate both goals with evidence:
  - *"Goal A: a public reference architecture for monolithic Quarkus apps using Domain-Driven Design and Hexagonal Architecture. The repo at `<URL>` is the artifact — clone it, study it, copy from it. Whether you use AI in your workflow or not, it's a working example of the kind of monolith you'll still be able to maintain three years from now."*
  - *"Goal B: proof that this style of architecture is a strong fit for agentic development. Four different CLIs, four different model providers, working from the same spec, in parallel — and they converged on the architectural commitments that matter. Any team using any of these tools can apply this pattern."*
- Plug the GitHub repo. Show the URL on screen. Note that all four agent branches are preserved for viewers to examine on their own time.
- *"Quarkus optimizes infrastructure. DDD optimizes the business. Together, they make a monolith that is **both** maintainable for humans and safe for agents — because the agents can't corrupt what's already protected by structure."*

---

## Recovery moments (if something goes sideways)

| Failure | Recovery |
|---|---|
| A Docker Sandbox crashes mid-run | Switch to that agent's pre-staged branch (`agent/<name>`) without commentary. Move on. |
| Live-coding compile error you can't fix in 30 seconds | Cut to the reference app's `Episode.java`, narrate the destination, return to the refactor on the next clean step. |
| Agent finishes a context but not all four | Examine only the contexts it completed. *"Three out of four contexts in 50 minutes — interesting that it prioritized X over Y."* Still useful as proof points for Goal B with caveats. |
| Two agents produce nearly identical output | Lean into it. *"That tells us something about the spec — when the spec is precise, agents converge. When it's loose, they diverge."* |

---

## Open questions (resolve before stream)

- [ ] Decide whether to live-narrate agent progress during 0:08 – 0:33 or stay heads-down on the IDE
- [ ] Decide whether to show seed data import in the open or treat it as already-loaded
- [ ] Length-test the live-coding refactor on a private dry run — 25 minutes is tight if you talk through every step
- [ ] Confirm the four CLIs in the Docker Sandbox actually all support YOLO/no-prompt mode (esp. Copilot CLI)
