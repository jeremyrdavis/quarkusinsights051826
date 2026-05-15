# Step 9: Persistence Adapter

The domain layer defines a port (`EpisodeRepository`). The application service depends on that port. Now we implement it: with Panache, PostgreSQL, and an explicit mapper that translates between domain objects and JPA entities. This step is where DDD and Quarkus meet — and where a lot of teams accidentally collapse the two by making the entity *be* the aggregate. We won't.

**Time:** ~30 min · **Files created:** 4 · **Tests added:** ~7

## TL;DR

Four files in `programming/infrastructure/persistence/`:

- **`EpisodeEntity.java`** — `@Entity extends PanacheEntityBase`, JPA annotations, raw types (`UUID`, `String`, `int`)
- **`AbstractEmbeddable.java`** — `@Embeddable` for the inside-aggregate Abstract
- **`EpisodeMapper.java`** — `@ApplicationScoped`, three methods: `toEntity`, `toDomain`, `applyTo`
- **`EpisodeRepositoryImpl.java`** — `@ApplicationScoped implements EpisodeRepository`, contains the only Panache calls in the codebase, translates DB constraint violations to domain exceptions

The mapper is the seam. Domain code never imports JPA; JPA code never imports `Episode`.

## Learning Objectives

By the end of this step you will be able to:

- Explain why the persistence model and the domain model are different objects, mapped at the seam
- Use Panache's `PanacheEntityBase` with an explicit UUID id (not the auto-generated Long)
- Decide whether to embed (`@Embedded`) or table-split (`@OneToOne`) an inside-aggregate entity
- Implement the **third place** of the three-place uniqueness rule: catch a DB constraint violation and re-throw the domain exception
- Test the adapter with `@QuarkusTest` and `@TestTransaction`

## Why This Matters

Naive Quarkus tutorials show you `class Episode extends PanacheEntity` and call it a day. One class, one set of annotations, persistence "for free." It's fast to write — and it's the trap.

The moment your `Episode` extends a JPA class:

- Your domain methods can be called with the entity in a detached or transient state, with surprising results.
- Your aggregate's invariants are routinely violated by JPA — no-args constructor, setter access via reflection, lazy-loading proxies.
- Your domain layer now imports `jakarta.persistence.*` everywhere, dragging in concerns that have nothing to do with the business.
- Your tests boot Quarkus to construct *any* aggregate, because constructing a JPA entity outside a persistence context is undefined.

The solution: **two models, one mapper.** The domain `Episode` is a pure POJO, no annotations. The persistence `EpisodeEntity` is a public-fields, JPA-managed mirror. The mapper translates between them. The cost: two files instead of one, and a translation step on every load and save. The benefit: each model is what it should be — domain enforces invariants, entity is a row in a table — and the test pyramid stays sane.

### Embedding vs. table-splitting

The Abstract is "inside" the Episode aggregate. At the relational level, you'd think it'd be a separate `abstract` table with a foreign key. But: an Abstract has no separate identity outside Episode, no other table references it, and the lifecycle is 1:1 (every Abstract belongs to exactly one Episode; an Episode has 0 or 1 Abstracts at a time).

So we **embed** it — the Abstract's fields become columns directly on the `episode` table:

```sql
CREATE TABLE episode (
    id UUID PRIMARY KEY,
    -- ... episode fields ...
    abstract_id UUID,                     -- embedded
    abstract_text VARCHAR(5000),          -- embedded
    abstract_submitted_at TIMESTAMP,      -- embedded
    -- ...
);
```

Embedding is the right call when:

- The inside-entity has no separate identity at the DB level
- You never query it independently of its parent
- The 1:1 relationship is enforced by the aggregate (never two abstracts on one episode)

If any of those flips later, you'd promote it to a separate table.

### The third place of the uniqueness rule

The application service (Step 8) checks `findByNumber(...)` before scheduling. But that check + insert isn't atomic — two concurrent calls can both see "no existing episode #42" and both try to insert. One wins, the other gets a `ConstraintViolationException` from Postgres.

Without intervention, that exception is opaque (`PSQLException: duplicate key value violates unique constraint`). The adapter's job is to **translate it back into the domain exception**:

```java
} catch (PersistenceException e) {
    translateConstraintViolations(e, episode.number());
    throw e;
}
```

If the underlying cause is a `ConstraintViolationException` on `uk_episode_number`, we re-throw `EpisodeNumberAlreadyExists`. Now the application service sees the same exception type regardless of whether the conflict was caught at the pre-check or by the race-protecting DB constraint. The REST layer can map one exception type, and the user gets a consistent error.

This is the **third place** of the three-place rule (service check, DB constraint, adapter translator). It's not redundancy — it's defense in depth, with one canonical error type at the top.

## Implementation

Four files. The two entity files can be created in either order; the mapper depends on both; the repository depends on the mapper.

### `AbstractEmbeddable.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.UUID;

@Embeddable
public class AbstractEmbeddable {

    @Column(name = "abstract_id")
    public UUID abstractId;

    @Column(name = "abstract_text", length = 5000)
    public String abstractText;

    @Column(name = "abstract_submitted_at")
    public Instant abstractSubmittedAt;

    public AbstractEmbeddable() {}
}
```

Three fields, all nullable. An Episode without a submitted abstract has all three columns null.

Notice the **public fields** — Panache's idiom is "no getters/setters." JPA writes to fields via reflection; there's no semantic difference between public fields and private-with-public-getters in this context, and public is less code.

### `EpisodeEntity.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "episode", uniqueConstraints = {
    @UniqueConstraint(name = "uk_episode_number", columnNames = "number")
})
public class EpisodeEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(nullable = false)
    public int number;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(name = "air_date", nullable = false)
    public LocalDate airDate;

    @Embedded
    public AbstractEmbeddable theAbstract;

    @ElementCollection
    @CollectionTable(
        name = "episode_presenter",
        joinColumns = @JoinColumn(name = "episode_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_episode_presenter",
            columnNames = {"episode_id", "person_id"})
    )
    @Column(name = "person_id", nullable = false)
    public Set<UUID> presenters = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "episode_speaker",
        joinColumns = @JoinColumn(name = "episode_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_episode_speaker",
            columnNames = {"episode_id", "person_id"})
    )
    @Column(name = "person_id", nullable = false)
    public Set<UUID> speakers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EpisodeStatus status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Version
    public long version;

    public EpisodeEntity() {}
}
```

Five things worth pausing on:

- **`extends PanacheEntityBase`, not `PanacheEntity`.** `PanacheEntity` gives you a `Long id` auto-generated by the DB. We want a `UUID id` we control (so the domain can mint IDs in `EpisodeId.random()` without involving the database). `PanacheEntityBase` is the version with no id assumption.
- **`@Table(uniqueConstraints = …)`** — this is the DB-level half of the uniqueness rule. Named `uk_episode_number` so the adapter can match on the name when translating.
- **`@ElementCollection` with composite uniqueness** — presenters and speakers each become a join table (`episode_presenter`, `episode_speaker`). The composite UNIQUE ensures (episode_id, person_id) is distinct, so the aggregate's "person can't be assigned twice" rule is also enforced at the DB.
- **`@Enumerated(EnumType.STRING)`** — store statuses as `'SCHEDULED'`, `'LIVE'`, etc., not as ordinals. Ordinals shift if you reorder the enum constants; strings don't.
- **`@Version` for optimistic locking** — concurrent updates to the same Episode bump the version; whichever transaction commits second sees a `StaleObjectStateException` and can retry.

### `EpisodeMapper.java`

The mapper has three methods because *load* and *save* and *update* are three different paths:

- **`toDomain(EpisodeEntity)` → `Episode`** — used by `findById`/`findByNumber` to rehydrate.
- **`toEntity(Episode)` → `EpisodeEntity`** — used by `save` when inserting a new aggregate.
- **`applyTo(Episode, EpisodeEntity)` → void** — used by `save` when updating an existing managed entity, preserving JPA-managed fields (notably `@Version`).

The full mapper is ~120 lines but mostly mechanical. Highlights:

```java
public Episode toDomain(EpisodeEntity entity) {
    Abstract theAbstract = null;
    if (entity.theAbstract != null && entity.theAbstract.abstractId != null) {
        theAbstract = new Abstract(
            new AbstractId(entity.theAbstract.abstractId),
            new AbstractText(entity.theAbstract.abstractText),
            entity.theAbstract.abstractSubmittedAt
        );
    }
    return Episode.rehydrate(
        new EpisodeId(entity.id),
        new EpisodeNumber(entity.number),
        new EpisodeTitle(entity.title),
        new AirDate(entity.airDate),
        theAbstract,
        entity.presenters.stream().map(PersonId::new).collect(Collectors.toSet()),
        entity.speakers.stream().map(PersonId::new).collect(Collectors.toSet()),
        entity.status,
        entity.createdAt,
        entity.updatedAt
    );
}
```

Notice **`Episode.rehydrate(...)`** — the factory we defined in Step 4 specifically for this. **Not** `Episode.schedule(...)`, which would record a fresh `EpisodeScheduled` event for an aggregate that already exists. Rehydration carries no domain event.

The `applyTo` method copies mutable fields onto a managed entity and explicitly *does not* touch `id`, `number`, `title`, `airDate`, `createdAt`, or `version`. Those are either immutable on the domain side or JPA-managed.

Get the full file from `module-01-solution/src/main/java/.../persistence/EpisodeMapper.java`.

### `EpisodeRepositoryImpl.java`

```java
package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.*;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EpisodeRepositoryImpl implements EpisodeRepository {

    private final EpisodeMapper mapper;

    @Inject
    public EpisodeRepositoryImpl(EpisodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Episode> findById(EpisodeId id) {
        EpisodeEntity entity = EpisodeEntity.findById(id.value());
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public Optional<Episode> findByNumber(EpisodeNumber number) {
        return EpisodeEntity.<EpisodeEntity>find("number", number.value())
            .firstResultOptional()
            .map(mapper::toDomain);
    }

    @Override
    public List<Episode> findByStatus(EpisodeStatus status) {
        List<EpisodeEntity> entities = EpisodeEntity.list("status", status);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void save(Episode episode) {
        try {
            EpisodeEntity existing = EpisodeEntity.findById(episode.id().value());
            if (existing == null) {
                EpisodeEntity entity = mapper.toEntity(episode);
                entity.persist();
            } else {
                mapper.applyTo(episode, existing);
            }
            EpisodeEntity.flush();
        } catch (PersistenceException e) {
            translateConstraintViolations(e, episode.number());
            throw e;
        }
    }

    private void translateConstraintViolations(PersistenceException e, EpisodeNumber attemptedNumber) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                if (name != null) {
                    String lower = name.toLowerCase();
                    if (lower.contains("uk_episode_number") || lower.contains("episode_number")) {
                        throw new EpisodeNumberAlreadyExists(attemptedNumber);
                    }
                }
                return;
            }
            cause = cause.getCause();
        }
    }
}
```

Save is the interesting method. Three cases:

1. **No existing entity with that ID** — fresh insert via `persist()`.
2. **Existing entity** — update via `applyTo(...)`, which copies mutable fields onto the managed entity. JPA flushes on transaction commit; we call `flush()` explicitly to force any constraint violation to surface now (and not at random later).
3. **Constraint violation thrown** — walk the cause chain to find the `ConstraintViolationException`, match the constraint name, re-throw `EpisodeNumberAlreadyExists`.

The constraint-name match accepts both `uk_episode_number` (the explicit name) and substring `episode_number` (a Postgres-generated fallback) — defensive against vendor naming variation.

The `Panache.findById` / `Panache.list` / `Panache.find` calls are the **only place in the codebase** where Panache shows up. Application code (Step 8) sees only the `EpisodeRepository` port. Everywhere else is domain.

## Testing

Persistence tests need a real database, so they use `@QuarkusTest` and `@TestTransaction`. Dev Services spins up the database automatically.

```java
package io.arrogantprogrammer.quarkusinsights.programming.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.programming.domain.*;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EpisodeRepositoryImplTest {

    @Inject EpisodeRepositoryImpl repository;

    @Test
    @TestTransaction
    void savesAndFindsByIdRoundTrip() {
        Episode episode = Episode.schedule(
            new EpisodeNumber(7),
            new EpisodeTitle("Round trip"),
            new AirDate(LocalDate.now().plusDays(1))
        );
        repository.save(episode);

        Optional<Episode> loaded = repository.findById(episode.id());
        assertTrue(loaded.isPresent());
        assertEquals(episode.id(), loaded.get().id());
        assertEquals(episode.number(), loaded.get().number());
    }

    @Test
    @TestTransaction
    void rejectsDuplicateNumberAtDbLevel() {
        EpisodeNumber number = new EpisodeNumber(42);
        Episode first = Episode.schedule(number, new EpisodeTitle("First"),
            new AirDate(LocalDate.now().plusDays(1)));
        repository.save(first);

        Episode duplicate = Episode.schedule(number, new EpisodeTitle("Duplicate"),
            new AirDate(LocalDate.now().plusDays(2)));
        assertThrows(EpisodeNumberAlreadyExists.class, () -> repository.save(duplicate));
    }

    // ... and more: findByNumber, findByStatus, update preserving version, etc.
}
```

`@TestTransaction` wraps the test in a transaction that's rolled back after the test — no need to clean up data manually. About seven such tests cover the full repository surface.

Also write a focused `EpisodeMapperTest.java` (plain JUnit, no `@QuarkusTest`) that exercises `toDomain`/`toEntity`/`applyTo` directly. The mapper is pure logic — testing it without booting Quarkus is faster and the assertions are clearer.

Run all the new tests:

```bash
./mvnw test -Dtest='EpisodeRepositoryImplTest,EpisodeMapperTest'
```

## You should now see

After Step 9, `./mvnw test` reports approximately:

```
[INFO] Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

(108 from earlier + ~11 mapper + repository tests.)

You can also start the app in dev mode and inspect the schema:

```bash
./mvnw quarkus:dev
# Open http://localhost:8080/q/dev/ in a browser
# Click "Datasources" → "Database (Dev UI)" to browse tables
```

You should see `episode`, `episode_presenter`, and `episode_speaker` tables with the columns you defined. They're empty — there's still no REST API to put data in. That's Step 10.

**Next:** Step 10 — the REST adapter. We expose seven endpoints, define request/response DTOs, write exception mappers that translate domain exceptions to HTTP codes, and finally hit `POST /api/episodes` to create an episode end-to-end.
