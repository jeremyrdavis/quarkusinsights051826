package io.arrogantprogrammer.quarkusinsights.admin.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end tests for the admin Episodes pages.
 */
@QuarkusTest
class EpisodeAdminControllerTest {

    private static int nextNumber = 50000;

    private synchronized int allocateNumber() {
        return nextNumber++;
    }

    private String registerPerson(String suffix) {
        return given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("firstName", "Test")
            .formParam("lastName", "Person" + suffix)
            .formParam("email", "test.person" + suffix + "@example.com")
            .formParam("bio", "y".repeat(80))
            .redirects().follow(false)
            .when().post("/admin/people/new")
            .then().statusCode(303)
            .extract().header("Location").replaceAll(".*/admin/people/", "");
    }

    @Test
    void listPageRenders() {
        given().when().get("/admin/episodes")
            .then().statusCode(200)
            .body(containsString("Episodes"))
            .body(containsString("New episode"));
    }

    @Test
    void composeFormRendersWithPickers() {
        given().when().get("/admin/episodes/new")
            .then().statusCode(200)
            .body(containsString("Compose a new episode"))
            .body(containsString("Presenters"))
            .body(containsString("Speakers"))
            .body(containsString("/admin/fragments/people-search"));
    }

    @Test
    void composeSubmitCreatesEpisodeAndRedirects() {
        String presenterId = registerPerson("p" + System.nanoTime());
        String speakerId = registerPerson("s" + System.nanoTime());
        int n = allocateNumber();

        String location = given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("number", n)
            .formParam("title", "Composed via admin")
            .formParam("airDate", LocalDate.now().plusDays(3).toString())
            .formParam("abstractText", "z".repeat(150))
            .formParam("presenterIds", presenterId)
            .formParam("speakerIds", speakerId)
            .redirects().follow(false)
            .when().post("/admin/episodes/new")
            .then().statusCode(303)
            .header("Location", matchesPattern(".*/admin/episodes/[0-9a-f\\-]{36}"))
            .extract().header("Location");

        String detailPath = location.replaceAll(".*(/admin/episodes/[^/]+)$", "$1");
        given().when().get(detailPath)
            .then().statusCode(200)
            .body(containsString("Composed via admin"))
            .body(containsString("SCHEDULED"));
    }

    @Test
    void composeFormReRendersWithErrorOnShortAbstract() {
        String presenterId = registerPerson("e" + System.nanoTime());
        String speakerId = registerPerson("f" + System.nanoTime());

        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("number", allocateNumber())
            .formParam("title", "Short abstract")
            .formParam("airDate", LocalDate.now().plusDays(1).toString())
            .formParam("abstractText", "too short")
            .formParam("presenterIds", presenterId)
            .formParam("speakerIds", speakerId)
            .redirects().follow(false)
            .when().post("/admin/episodes/new")
            .then().statusCode(400)
            .body(containsString("AbstractText"));
    }

    @Test
    void detailReturns404ForMissingEpisode() {
        given().when().get("/admin/episodes/" + UUID.randomUUID())
            .then().statusCode(404);
    }

    @Test
    void fullLifecycleFlowReachesPublicCatalog() {
        String presenterId = registerPerson("life" + System.nanoTime());
        String speakerId = registerPerson("flow" + System.nanoTime());
        int n = allocateNumber();
        String title = "Lifecycle smoke " + System.nanoTime();

        // Compose with airDate=today so we can go-live immediately.
        String location = given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("number", n)
            .formParam("title", title)
            .formParam("airDate", LocalDate.now().toString())
            .formParam("abstractText", "Lifecycle test abstract. ".repeat(8))
            .formParam("presenterIds", presenterId)
            .formParam("speakerIds", speakerId)
            .redirects().follow(false)
            .when().post("/admin/episodes/new")
            .then().statusCode(303)
            .extract().header("Location");
        String detailPath = location.replaceAll(".*(/admin/episodes/[^/]+)$", "$1");
        String episodeId = detailPath.replaceAll(".*/admin/episodes/", "");

        // Go live
        given().redirects().follow(false)
            .when().post("/admin/episodes/" + episodeId + "/go-live")
            .then().statusCode(303);

        // Publish
        given().redirects().follow(false)
            .when().post("/admin/episodes/" + episodeId + "/publish")
            .then().statusCode(303);

        // Admin detail now reports PUBLISHED
        given().when().get(detailPath)
            .then().statusCode(200)
            .body(containsString("PUBLISHED"))
            .body(containsString(title));

        // Public history page lists the published episode
        given().when().get("/episodes")
            .then().statusCode(200)
            .body(containsString(title));
    }
}
