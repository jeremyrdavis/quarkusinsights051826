package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end tests for {@link EpisodeResource}.
 *
 * <p>Boots Quarkus with Dev Services Postgres and exercises each
 * endpoint via HTTP using REST Assured. Each test schedules a fresh
 * Episode with a unique number to avoid cross-test pollution (the
 * test transaction-style isolation used by repository tests does
 * not work cleanly when the request crosses an HTTP boundary).
 */
@QuarkusTest
class EpisodeResourceTest {

    private static int nextNumber = 200;

    private synchronized int allocateNumber() {
        return nextNumber++;
    }

    private String scheduleEpisode(int number, String title, LocalDate airDate) {
        return given()
            .contentType("application/json")
            .body("{\"number\":" + number + ",\"title\":\"" + title + "\",\"airDate\":\"" + airDate + "\"}")
            .when().post("/episodes")
            .then().statusCode(201)
            .body("id", notNullValue())
            .body("number", equalTo(number))
            .body("status", equalTo("SCHEDULED"))
            .extract().path("id");
    }

    @Test
    void scheduleEndpointReturnsCreatedWithLocation() {
        int n = allocateNumber();
        given()
            .contentType("application/json")
            .body("{\"number\":" + n + ",\"title\":\"E2E\",\"airDate\":\"" + LocalDate.now().plusDays(7) + "\"}")
            .when().post("/episodes")
            .then().statusCode(201)
            .header("Location", notNullValue())
            .body("number", equalTo(n));
    }

    @Test
    void getEndpointReturnsScheduledEpisode() {
        int n = allocateNumber();
        String id = scheduleEpisode(n, "Get me", LocalDate.now().plusDays(1));

        given().when().get("/episodes/" + id)
            .then().statusCode(200)
            .body("id", equalTo(id))
            .body("number", equalTo(n));
    }

    @Test
    void submitAbstractEndpointReturnsUpdatedEpisode() {
        int n = allocateNumber();
        String id = scheduleEpisode(n, "Abstract test", LocalDate.now().plusDays(1));

        given()
            .contentType("application/json")
            .body("{\"text\":\"" + "a".repeat(150) + "\"}")
            .when().post("/episodes/" + id + "/abstract")
            .then().statusCode(200)
            .body("theAbstract.text", notNullValue())
            .body("theAbstract.id", notNullValue());
    }

    @Test
    void assignPresenterAndSpeakerEndpoints() {
        int n = allocateNumber();
        String id = scheduleEpisode(n, "Cast assignment", LocalDate.now().plusDays(1));
        UUID presenter = UUID.randomUUID();
        UUID speaker = UUID.randomUUID();

        given()
            .contentType("application/json")
            .body("{\"personId\":\"" + presenter + "\"}")
            .when().post("/episodes/" + id + "/presenters")
            .then().statusCode(200)
            .body("presenters", hasSize(1));

        given()
            .contentType("application/json")
            .body("{\"personId\":\"" + speaker + "\"}")
            .when().post("/episodes/" + id + "/speakers")
            .then().statusCode(200)
            .body("speakers", hasSize(1));
    }

    @Test
    void fullLifecycleScheduleAbstractCastGoLivePublish() {
        int n = allocateNumber();
        String id = scheduleEpisode(n, "Full lifecycle", LocalDate.now());

        given()
            .contentType("application/json")
            .body("{\"text\":\"" + "a".repeat(150) + "\"}")
            .when().post("/episodes/" + id + "/abstract")
            .then().statusCode(200);

        given()
            .contentType("application/json")
            .body("{\"personId\":\"" + UUID.randomUUID() + "\"}")
            .when().post("/episodes/" + id + "/presenters")
            .then().statusCode(200);

        given()
            .contentType("application/json")
            .body("{\"personId\":\"" + UUID.randomUUID() + "\"}")
            .when().post("/episodes/" + id + "/speakers")
            .then().statusCode(200);

        given().when().post("/episodes/" + id + "/go-live")
            .then().statusCode(200)
            .body("status", equalTo("LIVE"));

        given().when().post("/episodes/" + id + "/publish")
            .then().statusCode(200)
            .body("status", equalTo("PUBLISHED"));
    }

    @Test
    void cancelEndpointTransitionsToCanceled() {
        int n = allocateNumber();
        String id = scheduleEpisode(n, "Cancel target", LocalDate.now().plusDays(7));

        given()
            .contentType("application/json")
            .body("{\"reason\":\"Studio booking conflict\"}")
            .when().post("/episodes/" + id + "/cancel")
            .then().statusCode(200)
            .body("status", equalTo("CANCELED"));
    }

    @Test
    void getReturns404WhenEpisodeMissing() {
        given().when().get("/episodes/" + UUID.randomUUID())
            .then().statusCode(404)
            .body("error", equalTo("EpisodeNotFound"));
    }

    @Test
    void scheduleReturns400WhenAirDateInPast() {
        int n = allocateNumber();
        given()
            .contentType("application/json")
            .body("{\"number\":" + n + ",\"title\":\"Past\",\"airDate\":\"" + LocalDate.now().minusDays(1) + "\"}")
            .when().post("/episodes")
            .then().statusCode(400)
            .body("error", equalTo("AirDateInPast"));
    }

    @Test
    void scheduleReturns400WhenTitleBlank() {
        int n = allocateNumber();
        given()
            .contentType("application/json")
            .body("{\"number\":" + n + ",\"title\":\"  \",\"airDate\":\"" + LocalDate.now().plusDays(1) + "\"}")
            .when().post("/episodes")
            .then().statusCode(400)
            .body("error", equalTo("IllegalArgumentException"));
    }

    @Test
    void scheduleReturns409WhenNumberDuplicate() {
        int n = allocateNumber();
        scheduleEpisode(n, "First", LocalDate.now().plusDays(1));

        given()
            .contentType("application/json")
            .body("{\"number\":" + n + ",\"title\":\"Duplicate\",\"airDate\":\"" + LocalDate.now().plusDays(2) + "\"}")
            .when().post("/episodes")
            .then().statusCode(409)
            .body("error", equalTo("EpisodeNumberAlreadyExists"));
    }

    @Test
    void publishReturns409WhenScheduled() {
        int n = allocateNumber();
        String id = scheduleEpisode(n, "Not yet live", LocalDate.now().plusDays(7));

        given().when().post("/episodes/" + id + "/publish")
            .then().statusCode(409)
            .body("error", equalTo("IllegalEpisodeTransition"));
    }
}
