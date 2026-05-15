# Step 1: Value Objects

We're going to start with the smallest, simplest building blocks of the domain — value objects (VOs). A value object is anything whose identity comes entirely from its data: an `EpisodeNumber` of 42 is the same as any other `EpisodeNumber` of 42, the way a `5` is a `5`. By the end of this step you'll have six VOs that the rest of the workshop will lean on.

**Time:** ~20 min · **Files created:** 6 · **Tests added:** 33

## TL;DR

Create six records in `src/main/java/io/arrogantprogrammer/quarkusinsights/programming/domain/`:

```java
public record EpisodeNumber(int value) {
    public EpisodeNumber {
        if (value < 1) throw new IllegalArgumentException("EpisodeNumber must be at least 1, got: " + value);
    }
}
```

…plus `EpisodeTitle`, `AbstractId`, `AbstractText`, `AirDate`, and an `EpisodeStatus` enum. Each one enforces its invariants in the canonical constructor. Tests verify the invariants and equality-by-value.

## Learning Objectives

By the end of this step you will be able to:

- Explain what makes a Value Object different from an Entity
- Use a Java `record` to enforce invariants at construction time
- Distinguish "structural" invariants (live on the VO) from "temporal" or "cross-aggregate" invariants (don't)
- Write tests that pin down VO semantics: validation, accessors, equality

## Why This Matters

Most CRUD code passes raw `String`s and `int`s through every layer: `String email`, `int episodeNumber`, `String title`. The problem isn't typing — it's that **the rules** about what makes a valid email, or a valid episode number, live somewhere far from the data: in a validator class, a controller annotation, a database constraint, or worst of all, in nobody's head. The same `String` flows through ten methods and any of them could break the rule.

A Value Object closes that gap. An `EpisodeNumber` is *defined* by the rule "must be at least 1." You cannot construct one that violates the rule. Once you hold an `EpisodeNumber`, you don't validate it again — the type itself is the guarantee.

Eric Evans in *Domain-Driven Design* calls these "self-contained units of identity by attribute." Cockburn's hexagonal architecture calls the layer they live in "the application core." Whatever you call it, the discipline is the same: **push validation as close to the data as possible, then never repeat it.**

A second, quieter benefit: VOs are *evidence the team has language*. When the business says "episode number" and the code has `int n`, the business is in one room and the code is in another. When the code says `EpisodeNumber`, they're in the same room.

### What goes in a VO, what doesn't

| In | Out |
|---|---|
| Structural rules (non-null, non-blank, length, range) | Cross-aggregate rules ("must be unique across all episodes") |
| Format checks (UUID parsing, regex) | Temporal rules ("must be in the future") |
| Equality / hashing by value | Anything that requires reading another object |

`EpisodeNumber` enforces ≥1. It does **not** enforce uniqueness — that's a rule about the set of all episodes, which the aggregate (and database) handle, not the VO. `AirDate` accepts any `LocalDate` including past dates. The rule "must be in the future" lives in `Episode.schedule(...)`, which we'll build in Step 4.

This split is load-bearing. If you push too many rules into the VO, you can no longer rehydrate aggregates from the database (a 2024 episode has a past `AirDate`). If you push too few, validation scatters across the codebase. The line is: **does the rule depend only on this value, considered alone?** If yes, the VO enforces it.

## Implementation

Create each file in `src/main/java/io/arrogantprogrammer/quarkusinsights/programming/domain/`.

### 1. `EpisodeNumber.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Sequential identifier for an episode within the QuarkusInsights program.
 * Value Object — immutable, equals-by-value. Numbers start at 1.
 */
public record EpisodeNumber(int value) {
    public EpisodeNumber {
        if (value < 1) {
            throw new IllegalArgumentException("EpisodeNumber must be at least 1, got: " + value);
        }
    }
}
```

The `EpisodeNumber {` (no parameter list) syntax is the **compact constructor** introduced for records in Java 16. It runs before the implicit field assignment, so you can validate without re-naming parameters. If you've never seen it: it's not a typo.

Uniqueness is *not* checked here. We'll enforce it in `Episode.schedule(...)` (using an injected port) and back it up with a database `UNIQUE` constraint in Step 9.

### 2. `EpisodeTitle.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Display title for an episode. Non-null, non-blank, length 1..200 chars.
 */
public record EpisodeTitle(String value) {
    public EpisodeTitle {
        if (value == null) {
            throw new IllegalArgumentException("EpisodeTitle must not be null");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("EpisodeTitle must not be blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException(
                "EpisodeTitle must not exceed 200 characters, got: " + value.length());
        }
    }
}
```

Three checks, all about the string in isolation: null, blank, length. The 200-char cap is a product call (it fits a public catalog page nicely); changing it changes one constant in one place, not regex-hunting across the codebase.

### 3. `AbstractId.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import java.util.UUID;

/**
 * Unique identifier for an Abstract entity inside the Episode aggregate.
 * Lives inside programming/ (not shared/) because Abstract never travels
 * across bounded contexts.
 */
public record AbstractId(UUID value) {
    public AbstractId {
        if (value == null) {
            throw new IllegalArgumentException("AbstractId value must not be null");
        }
    }

    public static AbstractId random() {
        return new AbstractId(UUID.randomUUID());
    }

    public static AbstractId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("AbstractId string must not be null");
        }
        return new AbstractId(UUID.fromString(s));
    }
}
```

Two things worth pausing on:

- **`random()` and `fromString(String)` are factory methods on the VO itself**, not on a separate `AbstractIdFactory`. Records can have static methods; this keeps creation idioms together with the type.
- **AbstractId lives in `programming/domain/`, not `shared/`.** Compare to `EpisodeId`, which is in `shared/` because the People context needs to assign people to episodes (it passes `EpisodeId`s around without knowing what an `Episode` is). The Abstract entity, by contrast, never leaves the Episode aggregate — no other context cares about it — so its ID has no business in the shared kernel.

### 4. `AbstractText.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Body text of an episode's abstract. Length 100..5000 chars, non-blank.
 */
public record AbstractText(String value) {

    private static final int MIN_LENGTH = 100;
    private static final int MAX_LENGTH = 5000;

    public AbstractText {
        if (value == null) {
            throw new IllegalArgumentException("AbstractText must not be null");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("AbstractText must not be blank");
        }
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "AbstractText must be at least " + MIN_LENGTH
                + " characters, got: " + value.length());
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "AbstractText must not exceed " + MAX_LENGTH
                + " characters, got: " + value.length());
        }
    }
}
```

The minimum (100) is a *product* rule — "a real abstract has substance" — but it's still structural: it depends only on `value`. So it lives here.

### 5. `AirDate.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import java.time.LocalDate;

/**
 * Calendar date on which an episode airs. Accepts any LocalDate
 * including past dates — temporal rules belong to aggregate behavior.
 */
public record AirDate(LocalDate value) {
    public AirDate {
        if (value == null) {
            throw new IllegalArgumentException("AirDate must not be null");
        }
    }
}
```

This is the one where the discipline pays off. **It would be tempting to add `if (value.isBefore(LocalDate.now())) throw …` here.** Don't. Two reasons:

1. **Rehydration from the database** — when you load a 2024 episode from PostgreSQL, you need to construct an `AirDate` for January 2024. If the VO rejected past dates, you couldn't.
2. **"Now" is environmental** — pulling `LocalDate.now()` into a value object makes the VO un-testable except by mocking the clock. Aggregate behavior, where we *do* check "is this in the future?", can take a `Clock` parameter or an injected port.

### 6. `EpisodeStatus.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Lifecycle states of an Episode: SCHEDULED → LIVE → PUBLISHED,
 * with SCHEDULED → CANCELED as a branch. PUBLISHED and CANCELED
 * are terminal.
 */
public enum EpisodeStatus {
    SCHEDULED,
    LIVE,
    PUBLISHED,
    CANCELED
}
```

A status enum is a "degenerate" Value Object — equality, immutability, no identity, four allowed values. We could've used four `String` constants; the enum buys us exhaustiveness checking (`switch` warns on missing cases), no typos, and a name.

The transitions between these states are gated by methods on `Episode` (Steps 5 and 6). Nothing else should be writing to `status` directly.

## Testing

Each VO gets a focused test class in `src/test/java/io/arrogantprogrammer/quarkusinsights/programming/domain/`. **No `@QuarkusTest` annotation** — these are plain JUnit. The domain layer doesn't know what Quarkus is, and the tests shouldn't either; they're a few milliseconds each, and Quarkus boot is a few seconds.

### `EpisodeNumberTest.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EpisodeNumberTest {

    @Test
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeNumber(0));
    }

    @Test
    void rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeNumber(-1));
    }

    @Test
    void acceptsOne() {
        assertEquals(1, new EpisodeNumber(1).value());
    }

    @Test
    void acceptsLargeValues() {
        assertEquals(10_000, new EpisodeNumber(10_000).value());
    }

    @Test
    void equalsByValue() {
        assertEquals(new EpisodeNumber(42), new EpisodeNumber(42));
        assertEquals(new EpisodeNumber(42).hashCode(), new EpisodeNumber(42).hashCode());
    }
}
```

Five tests covering: lower-bound rejection (zero, negative), positive boundary (one), large-value smoke test, and equality-by-value. **Always include an `equalsByValue` test on a VO.** It's the test that documents "this is a value object, not an entity" and catches anyone who replaces the `record` with a `class` and forgets to override `equals`.

Write the same shape for the other five VOs:

- `EpisodeTitleTest` — reject null, blank, whitespace-only, >200 chars; accept boundary cases; equality
- `AbstractIdTest` — reject null; `random()` produces distinct IDs; `fromString` parses canonical UUIDs and rejects garbage; equality
- `AbstractTextTest` — reject null, blank, <100 chars, >5000 chars; accept boundary cases; equality
- `AirDateTest` — reject null; accept past, today, future; equality
- (No test needed for `EpisodeStatus` — the enum has no behavior yet)

Look at the matching files in `module-01-solution/src/test/java/...domain/` if you get stuck; they're the answer key.

Run just this step's tests:

```bash
./mvnw test -Dtest='*Test' -pl . 2>&1 | tail -20
```

Or more narrowly:

```bash
./mvnw test -Dtest='EpisodeNumberTest,EpisodeTitleTest,AbstractIdTest,AbstractTextTest,AirDateTest'
```

## You should now see

After Step 1, `./mvnw test` reports something like:

```
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

(33 = ~5 tests × 5 VOs + the smoke test + the 4 shared ID tests. Your count will vary slightly depending on how thoroughly you tested.)

The six files exist under `programming/domain/`, no other code references them yet, and the build is green.

**Next:** Step 2 — Domain Events. The events will use these VOs as their fields, so we built the alphabet before the sentences.
