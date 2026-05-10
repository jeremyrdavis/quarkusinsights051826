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
 * End-to-end tests for {@link CommentResource}.
 *
 * <p>Boots Quarkus with Dev Services Postgres and exercises each endpoint via
 * HTTP using REST Assured. Each test generates a unique {@code authorHandle}
 * to avoid cross-test pollution (HTTP-level tests share the Postgres instance;
 * no {@code @TestTransaction} rollback applies across the HTTP boundary).
 */
@QuarkusTest
class CommentResourceTest {

    private static final AtomicInteger SUFFIX = new AtomicInteger(0);

    private synchronized String nextHandle() {
        return "cuser" + SUFFIX.incrementAndGet();
    }

    /**
     * Posts a comment and returns the response body's {@code id} field.
     */
    private String submitComment(UUID episodeId, String authorHandle, String body) {
        return given()
            .contentType("application/json")
            .body("{\"episodeId\":\"" + episodeId + "\","
                + "\"authorHandle\":\"" + authorHandle + "\","
                + "\"body\":\"" + body + "\"}")
            .when().post("/comments")
            .then().statusCode(201)
            .body("id", notNullValue())
            .body("authorHandle", equalTo(authorHandle))
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
                + "\"body\":\"What a great episode!\"}")
            .when().post("/comments")
            .then().statusCode(201)
            .header("Location", notNullValue())
            .body("id", notNullValue())
            .body("episodeId", equalTo(episodeId.toString()))
            .body("authorHandle", equalTo(handle))
            .body("body", equalTo("What a great episode!"))
            .body("submittedAt", notNullValue());
    }

    @Test
    void getEndpointReturnsComment() {
        String handle = nextHandle();
        UUID episodeId = UUID.randomUUID();
        String id = submitComment(episodeId, handle, "Get me by id");

        given().when().get("/comments/" + id)
            .then().statusCode(200)
            .body("id", equalTo(id))
            .body("body", equalTo("Get me by id"));
    }

    @Test
    void getReturns404WhenCommentMissing() {
        given().when().get("/comments/" + UUID.randomUUID())
            .then().statusCode(404)
            .body("error", equalTo("CommentNotFound"));
    }

    @Test
    void editEndpointUpdatesBody() {
        String handle = nextHandle();
        UUID episodeId = UUID.randomUUID();
        String id = submitComment(episodeId, handle, "Original text");

        given()
            .contentType("application/json")
            .body("{\"body\":\"Edited text here\"}")
            .when().put("/comments/" + id)
            .then().statusCode(200)
            .body("id", equalTo(id))
            .body("body", equalTo("Edited text here"));

        // Verify via GET
        given().when().get("/comments/" + id)
            .then().statusCode(200)
            .body("body", equalTo("Edited text here"));
    }

    @Test
    void listByEpisodeReturnsChronological() {
        UUID episodeId = UUID.randomUUID();
        String h1 = nextHandle();
        String h2 = nextHandle();
        String h3 = nextHandle();

        String id1 = submitComment(episodeId, h1, "First comment");
        String id2 = submitComment(episodeId, h2, "Second comment");
        String id3 = submitComment(episodeId, h3, "Third comment");

        given()
            .when().get("/comments/by-episode/" + episodeId)
            .then().statusCode(200)
            .body("", hasSize(3))
            .body("[0].id", equalTo(id1))
            .body("[1].id", equalTo(id2))
            .body("[2].id", equalTo(id3));
    }

    @Test
    void listByEpisodeReturnsEmptyForUnknownEpisode() {
        given()
            .when().get("/comments/by-episode/" + UUID.randomUUID())
            .then().statusCode(200)
            .body("", hasSize(0));
    }
}
