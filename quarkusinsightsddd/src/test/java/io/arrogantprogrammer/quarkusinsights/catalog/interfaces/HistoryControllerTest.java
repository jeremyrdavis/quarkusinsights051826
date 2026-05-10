package io.arrogantprogrammer.quarkusinsights.catalog.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * End-to-end tests for {@link HistoryController}.
 *
 * <p>Boots Quarkus with Dev Services Postgres and exercises the episode
 * history page (GET /episodes) via HTTP using REST Assured. Published
 * episodes are created via the full lifecycle through the programming
 * REST API so the catalog projector picks them up.
 */
@QuarkusTest
class HistoryControllerTest {

    private static final AtomicInteger COUNTER = new AtomicInteger(920);

    private synchronized int nextNumber() {
        return COUNTER.getAndIncrement();
    }

    @Test
    void historyPageReturnsHtml() {
        given().when().get("/episodes")
            .then().statusCode(200)
            .contentType("text/html")
            .body(containsString("All Quarkus Insights episodes"));
    }

    @Test
    void historyPageWithPageAndSizeParameters() {
        given()
            .queryParam("page", "0")
            .queryParam("size", "10")
            .when().get("/episodes")
            .then().statusCode(200)
            .contentType("text/html")
            .body(containsString("All Quarkus Insights episodes"));
    }

    @Test
    void historyPageShowsPublishedEpisode() {
        int number = nextNumber();
        String title = "History Test Episode " + number;

        // Schedule the episode
        String id = given()
            .contentType("application/json")
            .body("{\"number\":" + number + ",\"title\":\"" + title + "\","
                + "\"airDate\":\"" + LocalDate.now() + "\"}")
            .when().post("/api/episodes")
            .then().statusCode(201)
            .extract().path("id");

        // Submit abstract (required for go-live)
        given()
            .contentType("application/json")
            .body("{\"text\":\"" + "a".repeat(150) + "\"}")
            .when().post("/api/episodes/" + id + "/abstract")
            .then().statusCode(200);

        // Assign presenter
        given()
            .contentType("application/json")
            .body("{\"personId\":\"" + UUID.randomUUID() + "\"}")
            .when().post("/api/episodes/" + id + "/presenters")
            .then().statusCode(200);

        // Assign speaker
        given()
            .contentType("application/json")
            .body("{\"personId\":\"" + UUID.randomUUID() + "\"}")
            .when().post("/api/episodes/" + id + "/speakers")
            .then().statusCode(200);

        // Go live
        given().when().post("/api/episodes/" + id + "/go-live")
            .then().statusCode(200);

        // Publish
        given().when().post("/api/episodes/" + id + "/publish")
            .then().statusCode(200);

        // The catalog projector stores a placeholder title ("Episode N") since EpisodeScheduled
        // does not carry the title text (known limitation of the current projector design).
        // Assert by episode number link which IS stored faithfully.
        given().when().get("/episodes")
            .then().statusCode(200)
            .body(containsString("/episodes/" + number));
    }
}
