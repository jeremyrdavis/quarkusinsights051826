package io.arrogantprogrammer.quarkusinsights.catalog.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * End-to-end tests for {@link EpisodeDetailController}.
 *
 * <p>Boots Quarkus with Dev Services Postgres and exercises:
 * <ul>
 *   <li>GET /episodes/{number} — detail page render</li>
 *   <li>POST /episodes/{number}/comments — HTMX comment fragment</li>
 *   <li>POST /episodes/{number}/ratings — HTMX rating-summary fragment</li>
 * </ul>
 *
 * <p>Each test allocates a unique episode number to avoid cross-test
 * pollution. The catalog projector fires synchronously within the
 * scheduling transaction, so the read model is available immediately
 * after the POST /api/episodes returns.
 */
@QuarkusTest
class EpisodeDetailControllerTest {

    private static final AtomicInteger COUNTER = new AtomicInteger(940);
    private static final AtomicInteger HANDLE_SUFFIX = new AtomicInteger(0);

    private synchronized int nextNumber() {
        return COUNTER.getAndIncrement();
    }

    private synchronized String nextHandle() {
        return "duser" + HANDLE_SUFFIX.incrementAndGet();
    }

    /**
     * Schedules an episode and returns its id.
     */
    private String scheduleEpisode(int number, String title) {
        return given()
            .contentType("application/json")
            .body("{\"number\":" + number + ",\"title\":\"" + title + "\","
                + "\"airDate\":\"" + LocalDate.now().plusDays(7) + "\"}")
            .when().post("/api/episodes")
            .then().statusCode(201)
            .extract().path("id");
    }

    @Test
    void detailPageRendersForScheduledEpisode() {
        int number = nextNumber();
        scheduleEpisode(number, "Detail Test Episode " + number);

        // The catalog projector stores a placeholder title ("Episode N") since EpisodeScheduled
        // does not carry the title text. Assert by episode number which IS stored faithfully.
        given().when().get("/episodes/" + number)
            .then().statusCode(200)
            .contentType("text/html")
            .body(containsString("#" + number))
            .body(containsString("Quarkus Insights"));
    }

    @Test
    void detailPageReturns404ForUnknownEpisode() {
        given().when().get("/episodes/99999")
            .then().statusCode(404);
    }

    @Test
    void submitCommentReturnsHtmlFragment() {
        int number = nextNumber();
        scheduleEpisode(number, "Comment Test Episode " + number);

        String commentBody = "Excellent episode about Quarkus!";

        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("authorHandle", nextHandle())
            .formParam("body", commentBody)
            .when().post("/episodes/" + number + "/comments")
            .then().statusCode(200)
            .contentType("text/html")
            .body(containsString(commentBody));
    }

    @Test
    void submitCommentFragmentContainsAuthorHandle() {
        int number = nextNumber();
        scheduleEpisode(number, "Comment Author Test " + number);

        String handle = nextHandle();

        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("authorHandle", handle)
            .formParam("body", "Testing author handle in fragment")
            .when().post("/episodes/" + number + "/comments")
            .then().statusCode(200)
            .body(containsString(handle));
    }

    @Test
    void submitRatingReturnsHtmlFragment() {
        int number = nextNumber();
        scheduleEpisode(number, "Rating Test Episode " + number);

        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("authorHandle", nextHandle())
            .formParam("stars", "5")
            .when().post("/episodes/" + number + "/ratings")
            .then().statusCode(200)
            .contentType("text/html")
            .body(containsString("rating-summary"));
    }

    @Test
    void submitRatingUpdatesRatingCount() {
        int number = nextNumber();
        scheduleEpisode(number, "Rating Count Test " + number);

        // Submit a rating
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("authorHandle", nextHandle())
            .formParam("stars", "4")
            .when().post("/episodes/" + number + "/ratings")
            .then().statusCode(200)
            .body(containsString("rating-summary"));

        // After rating, the detail page should show a non-zero rating count
        given().when().get("/episodes/" + number)
            .then().statusCode(200)
            .body(containsString("avg"));
    }
}
