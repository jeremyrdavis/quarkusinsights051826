# Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up a clean, public-ready monorepo for the QuarkusInsights DDD demo: remove agent-os symlinks and Quarkus scaffold, add supporting docs (`README`, `LICENSE`, abstract, CI), and create the shared kernel (`shared/` package with cross-context VO IDs + base `DomainEvent`) plus empty bounded-context package skeletons. The reference Quarkus app continues to compile and `./mvnw test` passes throughout.

**Architecture:** Single Maven module at `quarkusinsightsddd/` (existing scaffold). The shared kernel is intentionally minimal — only IDs that travel between contexts and the base `DomainEvent` interface. All other VOs (titles, dates, abstract text, etc.) stay inside their owning context. Bounded contexts get top-level packages with `package-info.java` markers; their internals get filled in by Plans 2–5.

**Tech Stack:** Quarkus 3.35.2 platform BOM, Java 25, Maven 3.9+, JUnit 5 (via `quarkus-junit`).

**Preconditions before executing:**
1. Working directory `/Users/jeremydavis/Workspace/QuarkusInsights/` is connected to the public GitHub repo with `main` checked out. If the remote already contains files (e.g., a generic `README`), merge those locally first; this plan adds/replaces files as specified.
2. `~/.claude/skills/quarkus-*` symlinks may stay in place (Quarkus mechanics skills, not DDD-opinion content). The `agent-os/` directory will be removed by Task 1.
3. Java 25 toolchain available; `./mvnw -version` succeeds inside `quarkusinsightsddd/`.
4. Docker available (for Dev Services Postgres in any test that boots Quarkus). Plan 1 itself does not require Postgres — VO tests are pure JUnit.

---

## File structure

### Files removed
- `agent-os/` (entire directory tree)
- `IDEA_PILE.md`
- `Domain-DrivenDesignAndHexagonalArchitectureWithQuarkusAndAgenticAI.docx`
- `~$main-Driven Design and Hexagonal Architecture with Quarkus (and Agentic AI).docx` (Word lockfile)
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/GreetingResource.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/MyEntity.java`
- `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/GreetingResourceTest.java`
- `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/GreetingResourceIT.java`

### Files created
- `LICENSE` (Apache 2.0 text)
- `README.md` (entry point for visitors)
- `docs/abstract.md` (markdown copy of the Word abstract)
- `.github/workflows/verify-reference.yml` (CI: `./mvnw test` on push to `main` and PRs)
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/DomainEvent.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeId.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/PersonId.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/CommentId.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/RatingId.java`
- `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeIdTest.java`
- `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/PersonIdTest.java`
- `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/CommentIdTest.java`
- `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/RatingIdTest.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/programming/package-info.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/people/package-info.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/engagement/package-info.java`
- `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/catalog/package-info.java`

### Files modified
- `.gitignore` (add `agent-os/`, `IDEA_PILE.md`, `*.docx`, `~$*.docx`, `docs/superpowers/` is already covered by user-local convention but make explicit if needed)

### Files unchanged
- `CLAUDE.md` (already gitignored, stays local)
- `SPEC.md` (does not yet exist — Plan 6)
- `SHOW_NOTES.md` (already exists at root)
- `docs/superpowers/specs/2026-05-09-quarkusinsights-ddd-demo-design.md` (the design doc itself — stays)
- `docs/superpowers/plans/2026-05-09-foundation.md` (this plan)
- `quarkusinsightsddd/pom.xml` (no dependency changes needed for Plan 1; existing deps suffice for shared-kernel POJOs and tests)

---

## Tasks

### Task 1: Remove the `agent-os/` directory

The `agent-os/standards/` content is under active development and would contaminate the reference architecture with potentially-incorrect DDD guidance. Removing it ensures the reference is built from principles.

**Files:**
- Delete: `/Users/jeremydavis/Workspace/QuarkusInsights/agent-os/` (entire tree)

- [ ] **Step 1.1: Confirm `agent-os/` exists and is a directory or symlink**

Run: `ls -la /Users/jeremydavis/Workspace/QuarkusInsights/agent-os 2>/dev/null && echo "exists" || echo "missing"`
Expected: `exists`

- [ ] **Step 1.2: Delete the directory tree**

Run: `rm -rf /Users/jeremydavis/Workspace/QuarkusInsights/agent-os`
Expected: no output, exit code 0

- [ ] **Step 1.3: Verify removal**

Run: `ls /Users/jeremydavis/Workspace/QuarkusInsights/agent-os 2>&1 | head`
Expected: `ls: cannot access ...: No such file or directory` (or equivalent)

- [ ] **Step 1.4: Stage the deletion**

Run: `cd /Users/jeremydavis/Workspace/QuarkusInsights && git add -A agent-os 2>/dev/null; git status --short | head`
Expected: any previously-tracked files under `agent-os/` show as `D` in status. If none were tracked, status will simply not list them — that's fine.

---

### Task 2: Update `.gitignore` to exclude in-progress and private files

Adds `agent-os/` (so re-introduction won't be tracked), `IDEA_PILE.md` (private brainstorming), and Word-related noise.

**Files:**
- Modify: `/Users/jeremydavis/Workspace/QuarkusInsights/.gitignore`

- [ ] **Step 2.1: Append exclusions to `.gitignore`**

Append exactly this block to the bottom of `/Users/jeremydavis/Workspace/QuarkusInsights/.gitignore`:

```
# QuarkusInsights demo — private and under-development content
agent-os/
IDEA_PILE.md

# Word lockfiles and source documents (markdown is the canonical form)
~$*.docx
*.docx
```

- [ ] **Step 2.2: Verify the exclusions are recognized**

Run: `cd /Users/jeremydavis/Workspace/QuarkusInsights && git check-ignore -v agent-os/ IDEA_PILE.md "Domain-DrivenDesignAndHexagonalArchitectureWithQuarkusAndAgenticAI.docx" 2>&1`
Expected: each path prints a line ending in the path itself (matched by the `.gitignore` rule).

- [ ] **Step 2.3: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add .gitignore
git commit -m "chore: gitignore agent-os/, IDEA_PILE, and Word artifacts"
```
Expected: one commit recorded.

---

### Task 3: Remove the Word document and lockfile

The `.docx` will be replaced by `docs/abstract.md` in Task 5. The `~$` lockfile is Word's open-file marker.

**Files:**
- Delete: `/Users/jeremydavis/Workspace/QuarkusInsights/Domain-DrivenDesignAndHexagonalArchitectureWithQuarkusAndAgenticAI.docx`
- Delete: `/Users/jeremydavis/Workspace/QuarkusInsights/~$main-Driven Design and Hexagonal Architecture with Quarkus (and Agentic AI).docx`

- [ ] **Step 3.1: Remove both files**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
rm -f "Domain-DrivenDesignAndHexagonalArchitectureWithQuarkusAndAgenticAI.docx"
rm -f "~"'$'"main-Driven Design and Hexagonal Architecture with Quarkus (and Agentic AI).docx"
```
Expected: no output. Both files gone.

- [ ] **Step 3.2: Verify no `.docx` remains**

Run: `ls /Users/jeremydavis/Workspace/QuarkusInsights/*.docx 2>&1`
Expected: `ls: ... *.docx: No such file or directory`

- [ ] **Step 3.3: Stage deletions**

Run: `cd /Users/jeremydavis/Workspace/QuarkusInsights && git add -A . 2>/dev/null && git status --short | head`
Expected: any `.docx` deletions appear as `D`. (If they were never tracked, status shows nothing for them — fine.)

---

### Task 4: Create `docs/abstract.md` with the markdown form of the talk abstract

Verbatim text from the original Word document, in markdown.

**Files:**
- Create: `/Users/jeremydavis/Workspace/QuarkusInsights/docs/abstract.md`

- [ ] **Step 4.1: Create `docs/` directory**

Run: `mkdir -p /Users/jeremydavis/Workspace/QuarkusInsights/docs`
Expected: no output, exit code 0.

- [ ] **Step 4.2: Write `docs/abstract.md`**

Write this exact content to `/Users/jeremydavis/Workspace/QuarkusInsights/docs/abstract.md`:

```markdown
# Domain-Driven Design and Hexagonal Architecture with Quarkus (and Agentic AI)

Quarkus makes it easy to build microservices applications. It is just as productive for teams building monolithic applications. Monolithic applications took a hit because traditional architectures like MVC tended to combine framework and business logic. By combining Quarkus with Domain-Driven Design, we can build monoliths that avoid the coupling that gave "monolith" a bad name.

Twenty-two years after *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Eric Evans' central idea still holds: isolate the domain, and everything else becomes a detail.

In this episode, we'll build a monolithic Quarkus application from scratch and use it to explore the fundamentals of Domain-Driven Design:

- What a Domain Model actually is (and what it is not)
- The difference between Entities and Value Objects
- How Aggregates protect business invariants
- How Services coordinate behavior without owning business rules
- Why Hexagonal Architecture makes your application extensible and isolates infrastructure code from your model

You'll see how to structure a Quarkus project so:

- Business rules live in the domain layer
- REST controllers stay thin
- Persistence and messaging remain adapters, not the core
- Your model reflects the ubiquitous language of the business

Then we'll take it one step further and unleash the agents. In an AI-assisted world, coding agents are excellent at generating controllers, repositories, and integration code. But they are pattern machines, not domain experts, and will happily distribute business rules across your codebase. When business logic lives inside Aggregates and Value Objects, agents can generate plumbing all day long without corrupting what matters.

After this session, you will be able to:

- Model a core domain in Quarkus using Entities, Value Objects, and Aggregates
- Encapsulate business rules so they don't leak into REST controllers or repositories
- Apply Hexagonal Architecture in a practical, non-academic way
- Understand why DDD makes AI-assisted development safer and more scalable
- Structure Quarkus projects so speed doesn't destroy clarity

Quarkus optimizes infrastructure. Domain-Driven Design optimizes the business.

No slides. No ivory-tower theories. Just Quarkus, code, and agentic-ready architecture.

---

## Short version

Quarkus makes it easy to build and ship applications. Domain-Driven Design optimizes business logic. In this episode, we'll build a monolithic Quarkus application using DDD and hexagonal architecture to keep our business rules clean, explicit, and protected. Then we'll introduce coding agents and show why strong domain boundaries are the key to making AI-assisted development scale. All code. No theory.
```

- [ ] **Step 4.3: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add docs/abstract.md
git rm --cached "Domain-DrivenDesignAndHexagonalArchitectureWithQuarkusAndAgenticAI.docx" 2>/dev/null || true
git commit -m "docs: add markdown abstract; remove Word source"
```
Expected: one commit recorded.

---

### Task 5: Create the `LICENSE` file (Apache 2.0)

Standard Apache License 2.0 boilerplate. This is the canonical text from the Apache Software Foundation.

**Files:**
- Create: `/Users/jeremydavis/Workspace/QuarkusInsights/LICENSE`

- [ ] **Step 5.1: Fetch the canonical Apache 2.0 license text**

Run:
```bash
curl -fsSL -o /Users/jeremydavis/Workspace/QuarkusInsights/LICENSE https://www.apache.org/licenses/LICENSE-2.0.txt
```
Expected: file downloaded, ~11.4 KB, no output on stdout/stderr.

- [ ] **Step 5.2: Verify the file**

Run: `head -3 /Users/jeremydavis/Workspace/QuarkusInsights/LICENSE`
Expected: first three lines are blank or whitespace-prefixed; line 5–6 contain `Apache License` and `Version 2.0, January 2004`.

Run: `wc -l /Users/jeremydavis/Workspace/QuarkusInsights/LICENSE`
Expected: ~202 lines.

- [ ] **Step 5.3: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add LICENSE
git commit -m "chore: add Apache 2.0 license"
```

---

### Task 6: Create `README.md` at repo root

The visitor's first 30 seconds. Sections for the agent branches and per-CLI rows are placeholders for now (filled by Plan 7's branch creation and post-stream); the rest is final.

**Files:**
- Create: `/Users/jeremydavis/Workspace/QuarkusInsights/README.md`

- [ ] **Step 6.1: Write `README.md`**

Write this exact content to `/Users/jeremydavis/Workspace/QuarkusInsights/README.md`:

```markdown
# Quarkus Insights — DDD + Hexagonal Architecture + Agentic AI

Reference repository for the QuarkusInsights episode
**Domain-Driven Design and Hexagonal Architecture with Quarkus (and Agentic AI)**.

This repo serves two co-equal purposes:

1. **A reference architecture** for building monolithic Quarkus applications using
   Domain-Driven Design and Hexagonal Architecture. Clone it, study it, copy it.
   Useful even if you have no interest in AI-assisted development.
2. **Proof that this architecture is a strong fit for agentic development.**
   Four CLI coding agents independently built the same application from
   [`SPEC.md`](SPEC.md) in parallel sandboxes — proof points that the architecture
   generalizes across CLIs, not a competition.

## Run the reference app

```bash
cd quarkusinsightsddd
./mvnw quarkus:dev
# Open http://localhost:8080
```

Requires Docker (for Dev Services Postgres) and a JDK with a Java 25 toolchain.

## What's in this repo

| File | What it is |
|---|---|
| [`SPEC.md`](SPEC.md) | The specification handed to four CLI coding agents |
| [`SHOW_NOTES.md`](SHOW_NOTES.md) | Stream timeline, presentation notes, architectural checklist |
| [`docs/abstract.md`](docs/abstract.md) | The talk's abstract |
| `quarkusinsightsddd/` | The reference application |

## The four agent outputs

Each agent ran in a Docker Sandbox in YOLO mode, given only `SPEC.md`:

| Agent | Branch |
|---|---|
| Claude Code | [`agent/claude-code`](../../tree/agent/claude-code) |
| Codex | [`agent/codex`](../../tree/agent/codex) |
| Gemini CLI | [`agent/gemini`](../../tree/agent/gemini) |
| Copilot CLI | [`agent/copilot`](../../tree/agent/copilot) |

Compare any branch against the reference:

```bash
git diff main agent/claude-code -- 'quarkusinsightsddd/src/**/Episode*.java'
```

## Replay the live-coding refactor

The naive-CRUD starting point is on `live/start`; the destination is on `live/end`.
The six refactor steps are six commits between them:

```bash
git log --oneline live/start..live/end
```

## License

Apache 2.0 — see [`LICENSE`](LICENSE).
```

- [ ] **Step 6.2: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add README.md
git commit -m "docs: add repo README"
```

---

### Task 7: Create the GitHub Actions CI workflow

Single workflow that runs `./mvnw -B test` on push to `main` and on PRs targeting `main`. Keeps the reference honest as later plans add code.

**Files:**
- Create: `/Users/jeremydavis/Workspace/QuarkusInsights/.github/workflows/verify-reference.yml`

- [ ] **Step 7.1: Create workflow directory**

Run: `mkdir -p /Users/jeremydavis/Workspace/QuarkusInsights/.github/workflows`
Expected: no output, exit code 0.

- [ ] **Step 7.2: Write the workflow file**

Write this exact content to `/Users/jeremydavis/Workspace/QuarkusInsights/.github/workflows/verify-reference.yml`:

```yaml
name: Verify reference

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 25

      - name: Cache Maven repository
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('quarkusinsightsddd/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Run tests
        working-directory: quarkusinsightsddd
        run: ./mvnw -B test
```

- [ ] **Step 7.3: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add .github/workflows/verify-reference.yml
git commit -m "ci: add reference verification workflow"
```

Note: the workflow won't pass yet (no tests exist after Task 8 removes the scaffold). It starts passing after Task 17 adds the shared-kernel tests. CI failures during plan execution are expected and acceptable.

---

### Task 8: Remove the Quarkus generator scaffold

Delete `GreetingResource`, `MyEntity`, and their tests so the reference codebase starts empty (apart from `pom.xml` and `application.properties`). The shared kernel will be the first real code added.

**Files:**
- Delete: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/GreetingResource.java`
- Delete: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/MyEntity.java`
- Delete: `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/GreetingResourceTest.java`
- Delete: `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/GreetingResourceIT.java`

- [ ] **Step 8.1: Remove the scaffold files**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
rm quarkusinsightsddd/src/main/java/io/arrogantprogrammer/GreetingResource.java
rm quarkusinsightsddd/src/main/java/io/arrogantprogrammer/MyEntity.java
rm quarkusinsightsddd/src/test/java/io/arrogantprogrammer/GreetingResourceTest.java
rm quarkusinsightsddd/src/test/java/io/arrogantprogrammer/GreetingResourceIT.java
rmdir quarkusinsightsddd/src/main/java/io/arrogantprogrammer 2>/dev/null || true
rmdir quarkusinsightsddd/src/test/java/io/arrogantprogrammer 2>/dev/null || true
```
Expected: no output. The empty `io/arrogantprogrammer` directories are removed only if empty (the trailing `|| true` makes failure non-fatal because they may not be empty after later tasks).

- [ ] **Step 8.2: Verify the build still compiles with no source code**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q compile
```
Expected: BUILD SUCCESS. Quarkus is fine with zero source files in `src/main/java/`.

- [ ] **Step 8.3: Verify `./mvnw test` passes (no tests = success)**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test
```
Expected: BUILD SUCCESS. Surefire runs zero tests, exits 0.

- [ ] **Step 8.4: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add -A quarkusinsightsddd/src
git commit -m "chore: remove Quarkus generator scaffold (GreetingResource, MyEntity)"
```

---

### Task 9: Write the failing test for `EpisodeId`

`EpisodeId` is a value object wrapping a `UUID`. It crosses bounded contexts (Programming produces it; Catalog consumes it via events; Engagement references it on Comment/Rating). Therefore it lives in `shared/`.

Tests cover: rejection of null, equality by value, factory method for random IDs, and string round-tripping.

**Files:**
- Create: `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeIdTest.java`

- [ ] **Step 9.1: Create the test directory**

Run:
```bash
mkdir -p /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared
```

- [ ] **Step 9.2: Write the failing test**

Write this exact content to `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeIdTest.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EpisodeIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EpisodeId a = new EpisodeId(uuid);
        EpisodeId b = new EpisodeId(uuid);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        EpisodeId a = EpisodeId.random();
        EpisodeId b = EpisodeId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "22222222-2222-2222-2222-222222222222";
        EpisodeId id = EpisodeId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> EpisodeId.fromString("not-a-uuid"));
    }
}
```

- [ ] **Step 9.3: Run the test and verify it fails to compile**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=EpisodeIdTest
```
Expected: BUILD FAILURE — compilation error: `cannot find symbol class EpisodeId`. This is the failing-test condition.

---

### Task 10: Implement `EpisodeId` to make the test pass

**Files:**
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeId.java`

- [ ] **Step 10.1: Create the source directory**

Run:
```bash
mkdir -p /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared
```

- [ ] **Step 10.2: Write the implementation**

Write this exact content to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeId.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.Objects;
import java.util.UUID;

public record EpisodeId(UUID value) {

    public EpisodeId {
        Objects.requireNonNull(value, "EpisodeId value must not be null");
    }

    public static EpisodeId random() {
        return new EpisodeId(UUID.randomUUID());
    }

    public static EpisodeId fromString(String s) {
        return new EpisodeId(UUID.fromString(s));
    }
}
```

Note on the rejection-of-null test: `Objects.requireNonNull` throws `NullPointerException`, which is a subclass of `IllegalArgumentException`? **No — it is not.** Verify:

`NullPointerException extends RuntimeException`, not `IllegalArgumentException`. The test `assertThrows(IllegalArgumentException.class, ...)` will fail if we use `requireNonNull` directly. So the constructor must throw `IllegalArgumentException` explicitly:

Replace the constructor body with:

```java
    public EpisodeId {
        if (value == null) {
            throw new IllegalArgumentException("EpisodeId value must not be null");
        }
    }
```

(Use this version, not `Objects.requireNonNull`.)

- [ ] **Step 10.3: Final source file**

The complete final file content is:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record EpisodeId(UUID value) {

    public EpisodeId {
        if (value == null) {
            throw new IllegalArgumentException("EpisodeId value must not be null");
        }
    }

    public static EpisodeId random() {
        return new EpisodeId(UUID.randomUUID());
    }

    public static EpisodeId fromString(String s) {
        return new EpisodeId(UUID.fromString(s));
    }
}
```

- [ ] **Step 10.4: Run the test and verify it passes**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=EpisodeIdTest
```
Expected: BUILD SUCCESS, 5 tests run, 0 failures.

- [ ] **Step 10.5: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeId.java
git add quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/EpisodeIdTest.java
git commit -m "feat(shared): add EpisodeId value object"
```

---

### Task 11: Add `PersonId` (test then implementation)

`PersonId` is identical in shape to `EpisodeId` — different type for type safety. Repeat the pattern.

**Files:**
- Create: `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/PersonIdTest.java`
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/PersonId.java`

- [ ] **Step 11.1: Write the failing test**

Write this exact content to `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/PersonIdTest.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new PersonId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID uuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        PersonId a = new PersonId(uuid);
        PersonId b = new PersonId(uuid);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        PersonId a = PersonId.random();
        PersonId b = PersonId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "44444444-4444-4444-4444-444444444444";
        PersonId id = PersonId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> PersonId.fromString("not-a-uuid"));
    }
}
```

- [ ] **Step 11.2: Verify it fails to compile**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=PersonIdTest
```
Expected: BUILD FAILURE — `cannot find symbol class PersonId`.

- [ ] **Step 11.3: Write the implementation**

Write this exact content to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/PersonId.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record PersonId(UUID value) {

    public PersonId {
        if (value == null) {
            throw new IllegalArgumentException("PersonId value must not be null");
        }
    }

    public static PersonId random() {
        return new PersonId(UUID.randomUUID());
    }

    public static PersonId fromString(String s) {
        return new PersonId(UUID.fromString(s));
    }
}
```

- [ ] **Step 11.4: Verify the test passes**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=PersonIdTest
```
Expected: BUILD SUCCESS, 5 tests run, 0 failures.

- [ ] **Step 11.5: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/PersonId.java
git add quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/PersonIdTest.java
git commit -m "feat(shared): add PersonId value object"
```

---

### Task 12: Add `CommentId` (test then implementation)

Same pattern as `EpisodeId` and `PersonId`.

**Files:**
- Create: `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/CommentIdTest.java`
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/CommentId.java`

- [ ] **Step 12.1: Write the failing test**

Write this exact content to `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/CommentIdTest.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new CommentId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID uuid = UUID.fromString("55555555-5555-5555-5555-555555555555");
        CommentId a = new CommentId(uuid);
        CommentId b = new CommentId(uuid);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        CommentId a = CommentId.random();
        CommentId b = CommentId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "66666666-6666-6666-6666-666666666666";
        CommentId id = CommentId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> CommentId.fromString("not-a-uuid"));
    }
}
```

- [ ] **Step 12.2: Verify it fails to compile**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=CommentIdTest
```
Expected: BUILD FAILURE — `cannot find symbol class CommentId`.

- [ ] **Step 12.3: Write the implementation**

Write this exact content to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/CommentId.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record CommentId(UUID value) {

    public CommentId {
        if (value == null) {
            throw new IllegalArgumentException("CommentId value must not be null");
        }
    }

    public static CommentId random() {
        return new CommentId(UUID.randomUUID());
    }

    public static CommentId fromString(String s) {
        return new CommentId(UUID.fromString(s));
    }
}
```

- [ ] **Step 12.4: Verify the test passes**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=CommentIdTest
```
Expected: BUILD SUCCESS, 5 tests run, 0 failures.

- [ ] **Step 12.5: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/CommentId.java
git add quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/CommentIdTest.java
git commit -m "feat(shared): add CommentId value object"
```

---

### Task 13: Add `RatingId` (test then implementation)

Same pattern.

**Files:**
- Create: `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/RatingIdTest.java`
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/RatingId.java`

- [ ] **Step 13.1: Write the failing test**

Write this exact content to `quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/RatingIdTest.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new RatingId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID uuid = UUID.fromString("77777777-7777-7777-7777-777777777777");
        RatingId a = new RatingId(uuid);
        RatingId b = new RatingId(uuid);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        RatingId a = RatingId.random();
        RatingId b = RatingId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "88888888-8888-8888-8888-888888888888";
        RatingId id = RatingId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> RatingId.fromString("not-a-uuid"));
    }
}
```

- [ ] **Step 13.2: Verify it fails to compile**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=RatingIdTest
```
Expected: BUILD FAILURE — `cannot find symbol class RatingId`.

- [ ] **Step 13.3: Write the implementation**

Write this exact content to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/RatingId.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record RatingId(UUID value) {

    public RatingId {
        if (value == null) {
            throw new IllegalArgumentException("RatingId value must not be null");
        }
    }

    public static RatingId random() {
        return new RatingId(UUID.randomUUID());
    }

    public static RatingId fromString(String s) {
        return new RatingId(UUID.fromString(s));
    }
}
```

- [ ] **Step 13.4: Verify the test passes**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q test -Dtest=RatingIdTest
```
Expected: BUILD SUCCESS, 5 tests run, 0 failures.

- [ ] **Step 13.5: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/RatingId.java
git add quarkusinsightsddd/src/test/java/io/arrogantprogrammer/quarkusinsights/shared/RatingIdTest.java
git commit -m "feat(shared): add RatingId value object"
```

---

### Task 14: Add the `DomainEvent` base interface

Every domain event in the system implements `DomainEvent`. The interface exposes only `occurredAt()` so cross-context subscribers can sort or audit without coupling to specific event types.

**Files:**
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/DomainEvent.java`

- [ ] **Step 14.1: Write the interface**

Write this exact content to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/DomainEvent.java`:

```java
package io.arrogantprogrammer.quarkusinsights.shared;

import java.time.Instant;

/**
 * Marker interface for all domain events. Concrete events are records inside
 * the producing context's `domain` package and carry only IDs and primitives —
 * they MUST NOT carry full aggregate references.
 */
public interface DomainEvent {

    /**
     * The instant at which this domain event was recorded by its aggregate.
     * Subscribers MAY use this to order or audit but MUST NOT use it to drive
     * business decisions (clock skew, replay, late arrivals make wall-time
     * unreliable for that purpose).
     */
    Instant occurredAt();
}
```

- [ ] **Step 14.2: Verify it compiles**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q compile
```
Expected: BUILD SUCCESS.

(No test for `DomainEvent` itself — it's an interface with one accessor and no behavior. It will be exercised by every concrete event in Plans 2–5.)

- [ ] **Step 14.3: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/shared/DomainEvent.java
git commit -m "feat(shared): add DomainEvent base interface"
```

---

### Task 15: Add `package-info.java` markers for the four bounded contexts

Empty marker files establish the package structure, document each context's responsibility, and make the layout visible in IDEs and on GitHub even before any code lives in those packages.

**Files:**
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/programming/package-info.java`
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/people/package-info.java`
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/engagement/package-info.java`
- Create: `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/catalog/package-info.java`

- [ ] **Step 15.1: Create the four package directories**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights
mkdir -p programming people engagement catalog
```

- [ ] **Step 15.2: Write `programming/package-info.java`**

Write to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/programming/package-info.java`:

```java
/**
 * Programming bounded context: the Episode aggregate (with inside-aggregate
 * Abstract entity), episode lifecycle (SCHEDULED → LIVE → PUBLISHED), and
 * presenter/speaker assignment. This context is the source of truth for
 * what shows exist and what state they are in.
 *
 * <p>Sub-packages: {@code domain}, {@code application},
 * {@code infrastructure}, {@code interfaces}.
 */
package io.arrogantprogrammer.quarkusinsights.programming;
```

- [ ] **Step 15.3: Write `people/package-info.java`**

Write to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/people/package-info.java`:

```java
/**
 * People bounded context: the Person aggregate. There are no separate
 * Speaker or Presenter aggregates — those are roles a Person plays on a
 * specific Episode, owned by the Programming context.
 *
 * <p>Sub-packages: {@code domain}, {@code application},
 * {@code infrastructure}, {@code interfaces}.
 */
package io.arrogantprogrammer.quarkusinsights.people;
```

- [ ] **Step 15.4: Write `engagement/package-info.java`**

Write to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/engagement/package-info.java`:

```java
/**
 * Engagement bounded context: the Comment and Rating aggregates. Both refer
 * to {@link io.arrogantprogrammer.quarkusinsights.shared.EpisodeId} but
 * never hold a reference to an Episode aggregate. The "one Rating per
 * (EpisodeId, AuthorHandle)" invariant is enforced in three places: the
 * SubmitRatingUseCase, a database UNIQUE constraint, and the repository
 * adapter's exception translation. See SPEC.md §3.6.
 *
 * <p>Sub-packages: {@code domain}, {@code application},
 * {@code infrastructure}, {@code interfaces}.
 */
package io.arrogantprogrammer.quarkusinsights.engagement;
```

- [ ] **Step 15.5: Write `catalog/package-info.java`**

Write to `quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/catalog/package-info.java`:

```java
/**
 * Public Catalog bounded context: the read side. Maintains a denormalized
 * {@code PublicEpisodeView} populated by event projectors that subscribe
 * to events from Programming, People, and Engagement. Owns the public-
 * facing UI (Qute templates + HTMX endpoints for posting comments and
 * ratings); writes are delegated to the Engagement context's use cases.
 *
 * <p>Sub-packages: {@code domain}, {@code application},
 * {@code infrastructure}, {@code interfaces}.
 */
package io.arrogantprogrammer.quarkusinsights.catalog;
```

- [ ] **Step 15.6: Verify the build**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 15.7: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/programming/package-info.java
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/people/package-info.java
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/engagement/package-info.java
git add quarkusinsightsddd/src/main/java/io/arrogantprogrammer/quarkusinsights/catalog/package-info.java
git commit -m "feat: scaffold four bounded-context packages with documentation"
```

---

### Task 16: Add a minimal `application.properties`

Sets the application name, HTTP port, and disables banner noise. Datasource/Dev Services configuration stays implicit — Quarkus brings up Postgres automatically when needed and we want that ergonomics.

**Files:**
- Modify: `quarkusinsightsddd/src/main/resources/application.properties` (currently empty)

- [ ] **Step 16.1: Confirm the file exists and is empty**

Run: `wc -c /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd/src/main/resources/application.properties`
Expected: `0` bytes (or whitespace-only).

- [ ] **Step 16.2: Write minimal config**

Write this exact content to `quarkusinsightsddd/src/main/resources/application.properties`:

```properties
quarkus.application.name=quarkusinsightsddd
quarkus.http.port=8080
quarkus.banner.enabled=false

# Domain layers must compile and test as POJOs without booting Quarkus.
# Tests under <context>/domain/ are pure JUnit; only adapter and end-to-end
# tests use @QuarkusTest.

# Hibernate ORM: validate schema in dev, drop-and-create in test, none in prod.
%dev.quarkus.hibernate-orm.database.generation=update
%test.quarkus.hibernate-orm.database.generation=drop-and-create
%prod.quarkus.hibernate-orm.database.generation=none

# Log SQL during dev for the live-coding refactor demo.
%dev.quarkus.hibernate-orm.log.sql=true
```

- [ ] **Step 16.3: Verify the build still succeeds**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B -q compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 16.4: Commit**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git add quarkusinsightsddd/src/main/resources/application.properties
git commit -m "chore: add minimal application.properties with profile-specific schema generation"
```

---

### Task 17: Verify the full test suite passes and the app boots

Final acceptance check for Plan 1.

- [ ] **Step 17.1: Run the full test suite**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
./mvnw -B test
```
Expected: BUILD SUCCESS. Tests run: 20 (5 each for `EpisodeIdTest`, `PersonIdTest`, `CommentIdTest`, `RatingIdTest`). 0 failures.

- [ ] **Step 17.2: Boot the app briefly to confirm it starts**

Run (Docker must be running for Dev Services):
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights/quarkusinsightsddd
timeout 60 ./mvnw quarkus:dev -Dquarkus.analytics.disabled=true </dev/null 2>&1 | head -40 &
QPID=$!
sleep 30
curl -fsS http://localhost:8080/q/health/ready 2>&1 | head
kill $QPID 2>/dev/null; wait $QPID 2>/dev/null; true
```
Expected: `curl` returns a JSON body with `"status": "UP"`. (If `curl` fails because Quarkus needs more than 30 s to boot Postgres, run the same shell again with longer `sleep`.)

- [ ] **Step 17.3: Final commit (no-op if everything was committed inline)**

Run:
```bash
cd /Users/jeremydavis/Workspace/QuarkusInsights
git status
```
Expected: `nothing to commit, working tree clean`.

If anything is uncommitted, identify why and commit it with a descriptive message before declaring Plan 1 complete.

---

## Definition of done for Plan 1

- [ ] `agent-os/` directory removed
- [ ] `IDEA_PILE.md`, both `.docx` files, and Word lockfiles gone from working tree (gitignored going forward)
- [ ] `LICENSE`, `README.md`, `docs/abstract.md`, `.github/workflows/verify-reference.yml` exist with content from this plan
- [ ] Quarkus generator scaffold removed (`GreetingResource`, `MyEntity`, their tests)
- [ ] Shared kernel: `EpisodeId`, `PersonId`, `CommentId`, `RatingId`, `DomainEvent` exist with tests
- [ ] All four bounded-context packages have `package-info.java` markers
- [ ] `application.properties` has profile-specific Hibernate generation settings
- [ ] `./mvnw test` runs 20 tests, all passing
- [ ] `./mvnw quarkus:dev` boots and `/q/health/ready` returns `UP`
- [ ] All commits pushed to `origin/main`
- [ ] CI workflow on GitHub passes the verify-reference job

---

## Self-review — placeholder scan & internal consistency

(Performed during plan authoring; issues fixed inline.)

1. **Spec coverage:** Foundation tasks cover §7.5 (existing-file disposition: agent-os, IDEA_PILE, .docx → .md), §7.4 (CI), §7.1 (LICENSE, README, gitignore, docs/abstract.md), §4.2 (shared kernel including EpisodeId/PersonId/CommentId/RatingId/DomainEvent), and §4.2 four-context package structure. Out of scope per design doc D13 (single Maven module): no module split.
2. **Placeholder scan:** No TBDs, TODOs, "implement later," or "similar to Task N" shorthand. All code blocks are complete and self-contained. Each step has exact commands with expected output.
3. **Type consistency:** All four ID VOs follow the same shape — `record <Name>Id(UUID value)`, factory methods `random()` and `fromString(String)`, `IllegalArgumentException` on null. `DomainEvent` interface signature is consistent with the design doc's "events carry IDs and primitives only."
4. **Caught and fixed inline:** Initially used `Objects.requireNonNull` in the `EpisodeId` constructor, which throws `NullPointerException` (not `IllegalArgumentException` as the test expects). Replaced with explicit `if (value == null) throw new IllegalArgumentException(...)`.
