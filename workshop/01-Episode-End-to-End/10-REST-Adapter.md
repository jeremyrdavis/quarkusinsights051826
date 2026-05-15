# Step 10: REST Adapter

The last layer. We expose seven endpoints, define request and response DTOs, write exception mappers that translate every domain exception into the right HTTP code, and run the app end-to-end. When this step is done, you can `POST /api/episodes`, walk an episode through its full lifecycle, and see real rows in the database — all enforced by the aggregate you built in Steps 4–6.

**Time:** ~30 min · **Files created:** ~13 · **Tests added:** ~10

## TL;DR

In `programming/interfaces/`, create:

- **`EpisodeResource.java`** — JAX-RS resource, eight endpoints (schedule, get, submitAbstract, assignPresenter/Speaker, goLive, publish, cancel)
- **DTOs** — `ScheduleEpisodeRequest`, `SubmitAbstractRequest`, `AssignPersonRequest`, `CancelRequest`, `EpisodeResponse` (with nested `AbstractView`)
- **`EpisodeResponseMapper.java`** — converts `Episode` aggregate → `EpisodeResponse` DTO
- **Exception mappers** — one per domain exception, mapping to 400 (invalid input), 404 (not found), or 409 (conflict)

After this step, `./mvnw quarkus:dev`, then `curl -X POST http://localhost:8080/api/episodes …` works end-to-end.

## Learning Objectives

By the end of this step you will be able to:

- Write a thin JAX-RS resource that does no business logic — just DTO ↔ command translation
- Choose the right HTTP status code for each domain exception
- Implement `ExceptionMapper<T>` providers that produce structured JSON error bodies
- Test REST endpoints with `@QuarkusTest` + REST Assured

## Why This Matters

The REST adapter is the *outside* of the hexagon — the place where the world's protocols meet the application's commands. Like the persistence adapter, it should do one thing well: translate.

Specifically:

- **Inbound**: JSON DTO → command (via VO constructors)
- **Outbound**: aggregate → JSON response DTO
- **Errors**: domain exception → HTTP status + JSON body

A common failure mode here is to let business logic creep in: "if status is SCHEDULED, allow this; otherwise return 409." Don't. **The aggregate already knows that rule.** The resource should call the aggregate (via the service), let the exception bubble, and let the right `ExceptionMapper` translate it. The rule lives in one place.

### Choosing status codes

Each domain exception maps to one HTTP status. The full mapping for the eight cases we built:

| Domain exception | HTTP status | Why |
|---|---|---|
| `EpisodeNotFound` | 404 Not Found | The resource doesn't exist |
| `EpisodeNumberAlreadyExists` | 409 Conflict | A constraint with another resource |
| `IllegalEpisodeTransition` | 409 Conflict | A state-machine conflict; retrying later won't help |
| `MissingAbstract` / `MissingPresenter` / `MissingSpeaker` | 409 Conflict | Precondition not met; resource is in wrong state |
| `AirDateInPast` / `AirDateNotYetReached` | 400 Bad Request | Validation: the input value isn't acceptable now |
| `IllegalArgumentException` (from VO constructors, e.g., `EpisodeNumber < 1`) | 400 Bad Request | Format/structural validation |

The split between 400 and 409 is the most contestable: both are "your request was rejected." The rule we follow:

- **400 Bad Request** — fix the *input* and the request will succeed. (E.g., supply a valid air date.)
- **409 Conflict** — fix the *state of the resource* and the request will succeed. (E.g., go live first, then publish.)

If retrying with the same input could succeed (after another action), it's 409. If not, it's 400.

### Why DTOs and not "just use the aggregate"

You could return the `Episode` aggregate directly and let Jackson serialize it. **Don't.** Two problems:

1. **The wire format becomes coupled to the domain model.** Renaming a field on `Episode` breaks every API consumer. Adding a new internal field accidentally exposes it.
2. **Domain types serialize awkwardly.** `EpisodeNumber` is a record wrapping `int`; serialized as `{"value": 42}` instead of `42`. Fixable with Jackson modules, but every fix is a leak across the wire/domain boundary.

A flat DTO with primitives is cheap, explicit, and stable. The mapper is half a page of code.

## Implementation

We'll do the DTOs first (the contract), then the mapper, then the resource, then the exception mappers. Total ~13 files, mostly small.

### Request DTOs

Four request shapes — one per body-carrying endpoint:

```java
// ScheduleEpisodeRequest.java
public record ScheduleEpisodeRequest(int number, String title, LocalDate airDate) {}

// SubmitAbstractRequest.java
public record SubmitAbstractRequest(String text) {}

// AssignPersonRequest.java (used for both presenters and speakers)
public record AssignPersonRequest(UUID personId) {}

// CancelRequest.java
public record CancelRequest(String reason) {}
```

Each is just a record with primitives. No annotations. JSON deserialization is automatic.

Notice these are **not** the commands. The DTO carries `int number` (the raw wire value); the command carries `EpisodeNumber number` (the validated VO). The resource translates one to the other — and the VO constructor is what enforces "≥ 1." If the request body has `"number": -3`, the `EpisodeNumber` constructor throws `IllegalArgumentException`, which becomes a 400 via the shared exception mapper.

### Response DTO

```java
// EpisodeResponse.java
public record EpisodeResponse(
    UUID id,
    int number,
    String title,
    LocalDate airDate,
    AbstractView theAbstract,   // null if no abstract submitted yet
    List<UUID> presenters,
    List<UUID> speakers,
    EpisodeStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public record AbstractView(UUID id, String text, Instant submittedAt) {}
}
```

Two nested records — outer `EpisodeResponse`, inner `AbstractView`. All primitives or basic JDK types. The `EpisodeStatus` enum gets serialized as a JSON string (`"SCHEDULED"`, `"LIVE"`, etc.) by Jackson's default — readable on the wire, parseable on the client.

### `EpisodeResponseMapper`

A small `@ApplicationScoped` bean that translates the aggregate to the DTO:

```java
package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.Collectors;

@ApplicationScoped
public class EpisodeResponseMapper {

    public EpisodeResponse toResponse(Episode episode) {
        EpisodeResponse.AbstractView abstractView = null;
        if (episode.theAbstract() != null) {
            abstractView = new EpisodeResponse.AbstractView(
                episode.theAbstract().id().value(),
                episode.theAbstract().text().value(),
                episode.theAbstract().submittedAt()
            );
        }
        return new EpisodeResponse(
            episode.id().value(),
            episode.number().value(),
            episode.title().value(),
            episode.airDate().value(),
            abstractView,
            episode.presenters().stream().map(PersonId::value).collect(Collectors.toList()),
            episode.speakers().stream().map(PersonId::value).collect(Collectors.toList()),
            episode.status(),
            episode.createdAt(),
            episode.updatedAt()
        );
    }
}
```

Mechanical: unwrap each VO with `.value()`, build a flat record. The mirror image of what `EpisodeMapper.toDomain(...)` did in Step 9 (which unwrapped DB rows back into VOs).

### `EpisodeResource`

```java
package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.application.*;
import io.arrogantprogrammer.quarkusinsights.programming.domain.*;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

@Path("/api/episodes")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EpisodeResource {

    @Inject EpisodeService episodeService;
    @Inject EpisodeRepository repository;
    @Inject EpisodeResponseMapper responseMapper;

    @POST
    @Transactional
    public Response schedule(ScheduleEpisodeRequest request) {
        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            new EpisodeNumber(request.number()),
            new EpisodeTitle(request.title()),
            new AirDate(request.airDate())
        );
        EpisodeId id = episodeService.schedule(cmd);
        Episode created = loadOrThrow(id);
        return Response.created(URI.create("/api/episodes/" + id.value()))
            .entity(responseMapper.toResponse(created))
            .build();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public EpisodeResponse get(@PathParam("id") UUID id) {
        return responseMapper.toResponse(loadOrThrow(new EpisodeId(id)));
    }

    @POST
    @Path("/{id}/abstract")
    @Transactional
    public EpisodeResponse submitAbstract(@PathParam("id") UUID id, SubmitAbstractRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.submitAbstract(
            new SubmitAbstractCommand(episodeId, new AbstractText(request.text())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    @POST
    @Path("/{id}/presenters")
    @Transactional
    public EpisodeResponse assignPresenter(@PathParam("id") UUID id, AssignPersonRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.assignPresenter(
            new AssignPresenterCommand(episodeId, new PersonId(request.personId())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    @POST
    @Path("/{id}/speakers")
    @Transactional
    public EpisodeResponse assignSpeaker(@PathParam("id") UUID id, AssignPersonRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.assignSpeaker(
            new AssignSpeakerCommand(episodeId, new PersonId(request.personId())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    @POST
    @Path("/{id}/go-live")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public EpisodeResponse goLive(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.goLive(new GoLiveCommand(episodeId));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    @POST
    @Path("/{id}/publish")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public EpisodeResponse publish(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.publish(new PublishEpisodeCommand(episodeId));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    @POST
    @Path("/{id}/cancel")
    @Transactional
    public EpisodeResponse cancel(@PathParam("id") UUID id, CancelRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.cancel(new CancelEpisodeCommand(episodeId, request.reason()));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    private Episode loadOrThrow(EpisodeId id) {
        return repository.findById(id).orElseThrow(() -> new EpisodeNotFound(id));
    }
}
```

Observations:

- **Every method is `@Transactional`** — the service call and the subsequent `findById` (for the response) share a session, so we read consistent state.
- **`@Consumes(MediaType.WILDCARD)` on `goLive` and `publish`** — these endpoints have empty bodies; the wildcard avoids a 415 if the client sends `Content-Type: application/octet-stream` or omits it.
- **`schedule` returns `Response` with a `Location` header.** REST convention for create operations: 201 + `Location` pointing at the new resource. The other endpoints return the DTO directly; JAX-RS picks 200 for those.

### Exception mappers

One `@Provider`-annotated class per domain exception. They all follow the same shape:

```java
@Provider
public class EpisodeNotFoundMapper implements ExceptionMapper<EpisodeNotFound> {
    @Override
    public Response toResponse(EpisodeNotFound exception) {
        return Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "EpisodeNotFound",
                "message", exception.getMessage()
            ))
            .build();
    }
}
```

Write one for each of:

- `EpisodeNotFoundMapper` → **404**
- `EpisodeNumberAlreadyExistsMapper` → **409**
- `IllegalEpisodeTransitionMapper` → **409**
- `MissingAbstractMapper` → **409**
- `MissingPresenterMapper` → **409**
- `MissingSpeakerMapper` → **409**
- `AirDateInPastMapper` → **400**
- `AirDateNotYetReachedMapper` → **400**

Plus the `IllegalArgumentExceptionMapper` that already exists in `shared/interfaces/` — it catches everything that VO constructors throw (`EpisodeNumber < 1`, blank title, etc.) and returns **400**.

That's eight new files in this package + the one inherited from shared, for nine total mappers.

Each mapper's body is essentially identical — `Map.of("error", "<Name>", "message", exception.getMessage())`. Resist the urge to extract a shared base class; it tangles the type parameters and saves you eight lines you'll never read again.

## Testing

REST tests use `@QuarkusTest` + REST Assured. The full app boots (so the resource, service, repository, and exception mappers all wire up), Dev Services brings up Postgres, and tests issue real HTTP calls against `localhost:8081`.

```java
package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class EpisodeResourceTest {

    @Test
    void scheduleReturns201AndLocation() {
        given()
            .contentType("application/json")
            .body(Map.of(
                "number", 9001,                    // high to avoid collisions with other tests
                "title", "REST Test",
                "airDate", LocalDate.now().plusDays(7).toString()
            ))
            .when().post("/api/episodes")
            .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("number", equalTo(9001))
                .body("status", equalTo("SCHEDULED"));
    }

    @Test
    void schedulingDuplicateNumberReturns409() {
        // schedule once, then try again with same number
        // expect 409 + error: "EpisodeNumberAlreadyExists"
    }

    @Test
    void schedulingWithPastAirDateReturns400() {
        given()
            .contentType("application/json")
            .body(Map.of(
                "number", 9002, "title", "Past",
                "airDate", LocalDate.now().minusDays(1).toString()
            ))
            .when().post("/api/episodes")
            .then()
                .statusCode(400)
                .body("error", equalTo("AirDateInPast"));
    }

    // ... more tests covering each endpoint and each error case
}
```

About ten tests cover the API surface. The full file lives in `module-01-solution/src/test/java/.../EpisodeResourceTest.java`.

**Tip on test isolation:** REST tests use a real database and don't `@TestTransaction` (because they hit the HTTP layer, which has its own tx boundary). Use **unique episode numbers per test** (e.g., 9001, 9002, …) so tests don't collide with each other or with earlier runs. Some teams use random UUIDs in titles for the same reason.

## End-to-end smoke test (the moment of truth)

Start the app in dev mode:

```bash
./mvnw quarkus:dev
```

In another terminal, walk an episode through its lifecycle:

```bash
# 1. Schedule
curl -i -X POST http://localhost:8080/api/episodes \
  -H 'Content-Type: application/json' \
  -d '{"number":1,"title":"Pilot","airDate":"2026-06-01"}'
# → 201 Created, Location: /api/episodes/<UUID>

# Save the UUID as $ID for the next steps:
ID="<paste the id from the response>"

# 2. Submit abstract
curl -X POST "http://localhost:8080/api/episodes/$ID/abstract" \
  -H 'Content-Type: application/json' \
  -d "{\"text\":\"$(printf 'A'%.0s {1..150})\"}"

# 3. Assign presenter and speaker
curl -X POST "http://localhost:8080/api/episodes/$ID/presenters" \
  -H 'Content-Type: application/json' \
  -d "{\"personId\":\"$(uuidgen)\"}"
curl -X POST "http://localhost:8080/api/episodes/$ID/speakers" \
  -H 'Content-Type: application/json' \
  -d "{\"personId\":\"$(uuidgen)\"}"

# 4. Go live (only works if today >= airDate — adjust airDate above to today's date for the demo)
curl -X POST "http://localhost:8080/api/episodes/$ID/go-live"

# 5. Publish
curl -X POST "http://localhost:8080/api/episodes/$ID/publish"

# 6. GET — see the final state
curl "http://localhost:8080/api/episodes/$ID"
# → status: "PUBLISHED"
```

Try the error cases too:

```bash
# Duplicate number → 409 + error: "EpisodeNumberAlreadyExists"
curl -i -X POST http://localhost:8080/api/episodes \
  -H 'Content-Type: application/json' \
  -d '{"number":1,"title":"Duplicate","airDate":"2026-06-02"}'

# Publish before goLive → 409 + error: "IllegalEpisodeTransition"
# (need a scheduled-but-not-live episode for this)

# Negative episode number → 400 (caught by IllegalArgumentExceptionMapper)
curl -i -X POST http://localhost:8080/api/episodes \
  -H 'Content-Type: application/json' \
  -d '{"number":-1,"title":"Bad","airDate":"2026-06-03"}'
```

Each error response includes a structured JSON body identifying which domain exception fired. Front-end clients can switch on `error` rather than parsing the message text.

## You should now see

After Step 10, `./mvnw test` reports approximately:

```
[INFO] Tests run: 129, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

(119 from earlier + ~10 REST tests.)

In the browser at <http://localhost:8080/q/dev/>, the Quarkus Dev UI shows your endpoints under "REST Endpoints," your tables under "Datasources," and your CDI beans (including the projector — actually no, we didn't build projectors; that's the catalog context in the full reference app, omitted from this workshop).

**You're done.** The Episode aggregate is built, tested, persisted, and exposed end-to-end. You now have:

- A pure-POJO domain model that enforces every business rule
- An application service that orchestrates without containing logic
- A persistence adapter that translates between domain and JPA
- A REST adapter that translates between HTTP and commands
- 129 tests, none of which mock business logic — they all run against the real aggregate (most without booting Quarkus at all)

This is the Programming context. The full QuarkusInsights reference app at `quarkusinsightsddd/` (one directory up from this workshop) goes further: it adds People, Engagement, and Catalog bounded contexts, CQRS projectors, and a Qute+HTMX UI on top of everything you just built. The patterns are identical; the scope is wider. Go read it.

**Congratulations.** You've now built a hexagonal, DDD-shaped Quarkus monolith from scratch, on top of an architecture that's safe enough for coding agents to extend without smearing business rules across your codebase. That was the whole point.
