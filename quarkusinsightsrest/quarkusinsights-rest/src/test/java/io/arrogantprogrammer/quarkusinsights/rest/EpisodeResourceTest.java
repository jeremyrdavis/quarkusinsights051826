package io.arrogantprogrammer.quarkusinsights.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class EpisodeResourceTest {

    @Test
    public void testCreateEpisodeHtmx() {
        given()
                .header("Content-Type", "application/json")
                .header("HX-Request", "true")
                .body("{\"title\": \"HTMX Episode\", \"description\": \"Testing HTMX POST\", \"url\": \"http://example.com\", \"airDate\": \"2026-05-17\"}")
                .when()
                .post("/episodes")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body(containsString("Created episode: HTMX Episode"));
    }

    @Test
    public void testGetEpisodesView() {
        given()
                .when()
                .get("/episodes/view")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body(containsString("Create New Episode"))
                .body(containsString("All Episodes"));
    }
}
