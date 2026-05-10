package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end tests for {@link RatingResource}.
 *
 * <p>Boots Quarkus with Dev Services Postgres and exercises each endpoint via
 * HTTP using REST Assured. Each test generates a unique {@code authorHandle}
 * to avoid cross-test pollution (HTTP-level tests share the Postgres instance;
 * no {@code @TestTransaction} rollback applies across the HTTP boundary).
 *
 * <p>The critical test is {@link #submitReturns409WhenAuthorAlreadyRated} —
 * the end-to-end validation of SPEC.md §3.6 across all three enforcement layers.
 */
@QuarkusTest
class RatingResourceTest {

    private static final AtomicInteger SUFFIX = new AtomicInteger(0);

    private synchronized String nextHandle() {
        return "ruser" + SUFFIX.incrementAndGet();
    }

    /**
     * Posts a rating and returns the response body's {@code id} field.
     */
    private String submitRating(UUID episodeId, String authorHandle, int stars) {
        return given()
            .contentType("application/json")
            .body("{\"episodeId\":\"" + episodeId + "\","
                + "\"authorHandle\":\"" + authorHandle + "\","
                + "\"stars\":" + stars + "}")
            .when().post("/ratings")
            .then().statusCode(201)
            .body("id", notNullValue())
            .body("stars", equalTo(stars))
            .extract().path("id");
    }

    @Test
    void submitReturnsCreatedWithLocation() {
        String handle = nextHandle();
        UUID episodeId = UUID.randomUUID();

        given()
            .contentType("application/json")
            .body("{\"episodeId\":\"" + episodeId + "\","
                + "\"authorHandle\":\"" + handle + "\","
                + "\"stars\":5}")
            .when().post("/ratings")
            .then().statusCode(201)
            .header("Location", notNullValue())
            .body("id", notNullValue())
            .body("episodeId", equalTo(episodeId.toString()))
            .body("authorHandle", equalTo(handle))
            .body("stars", equalTo(5))
            .body("submittedAt", notNullValue());
    }

    @Test
    void getEndpointReturnsRating() {
        String handle = nextHandle();
        UUID episodeId = UUID.randomUUID();
        String id = submitRating(episodeId, handle, 3);

        given().when().get("/ratings/" + id)
            .then().statusCode(200)
            .body("id", equalTo(id))
            .body("stars", equalTo(3));
    }

    @Test
    void getReturns404WhenRatingMissing() {
        given().when().get("/ratings/" + UUID.randomUUID())
            .then().statusCode(404)
            .body("error", equalTo("RatingNotFound"));
    }

    @Test
    void listByEpisodeReturnsAll() {
        UUID episodeId = UUID.randomUUID();
        String h1 = nextHandle();
        String h2 = nextHandle();
        String h3 = nextHandle();

        String id1 = submitRating(episodeId, h1, 5);
        String id2 = submitRating(episodeId, h2, 3);
        String id3 = submitRating(episodeId, h3, 1);

        given()
            .when().get("/ratings/by-episode/" + episodeId)
            .then().statusCode(200)
            .body("", hasSize(3))
            .body("[0].id", equalTo(id1))
            .body("[1].id", equalTo(id2))
            .body("[2].id", equalTo(id3));
    }

    @Test
    void listByEpisodeReturnsEmptyForUnknownEpisode() {
        given()
            .when().get("/ratings/by-episode/" + UUID.randomUUID())
            .then().statusCode(200)
            .body("", hasSize(0));
    }

    /**
     * CRITICAL §3.6 END-TO-END TEST: validates the full three-place rule
     * across all enforcement layers.
     *
     * <p>Posts a rating for (episodeId, author), then attempts a second POST
     * with the same (episodeId, author). The second POST must return 409, the
     * body must have {@code error: "AlreadyRated"}, and the body must include
     * {@code existingRatingId} matching the first POST's id.
     */
    @Test
    void submitReturns409WhenAuthorAlreadyRated() {
        String handle = nextHandle();
        UUID episodeId = UUID.randomUUID();

        // First rating succeeds — captures its id
        String firstId = submitRating(episodeId, handle, 4);

        // Second attempt with same (episodeId, handle) must be rejected
        given()
            .contentType("application/json")
            .body("{\"episodeId\":\"" + episodeId + "\","
                + "\"authorHandle\":\"" + handle + "\","
                + "\"stars\":2}")
            .when().post("/ratings")
            .then().statusCode(409)
            .body("error", equalTo("AlreadyRated"))
            .body("existingRatingId", equalTo(firstId));
    }
}
