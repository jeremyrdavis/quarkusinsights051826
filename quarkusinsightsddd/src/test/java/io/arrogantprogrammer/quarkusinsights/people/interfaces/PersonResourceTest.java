package io.arrogantprogrammer.quarkusinsights.people.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end tests for {@link PersonResource}.
 *
 * <p>Boots Quarkus with Dev Services Postgres and exercises each
 * endpoint via HTTP using REST Assured. Each test uses a unique email
 * address (via {@link #nextHandle()}) to avoid cross-test pollution
 * (the test-transaction isolation used by repository tests does not
 * work cleanly when the request crosses an HTTP boundary).
 */
@QuarkusTest
class PersonResourceTest {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private synchronized String nextHandle() {
        return "person" + COUNTER.getAndIncrement() + "@example.com";
    }

    private static final String VALID_BIO = "A".repeat(50);

    /**
     * Helper to register a person via POST /people and return the UUID.
     */
    private String registerPerson(String email) {
        return given()
            .contentType("application/json")
            .body("{\"name\":{\"first\":\"Holly\",\"last\":\"Cummins\"}," +
                  "\"email\":\"" + email + "\"," +
                  "\"bio\":\"" + VALID_BIO + "\"}")
            .when().post("/api/people")
            .then().statusCode(201)
            .body("id", notNullValue())
            .extract().path("id");
    }

    @Test
    void registerEndpointReturnsCreatedWithLocation() {
        String email = nextHandle();
        given()
            .contentType("application/json")
            .body("{\"name\":{\"first\":\"Holly\",\"last\":\"Cummins\"}," +
                  "\"email\":\"" + email + "\"," +
                  "\"bio\":\"" + VALID_BIO + "\"}")
            .when().post("/api/people")
            .then().statusCode(201)
            .header("Location", notNullValue())
            .body("id", notNullValue())
            .body("name.first", equalTo("Holly"))
            .body("name.last", equalTo("Cummins"))
            .body("email", equalTo(email));
    }

    @Test
    void getEndpointReturnsPerson() {
        String email = nextHandle();
        String id = registerPerson(email);

        given().when().get("/api/people/" + id)
            .then().statusCode(200)
            .body("id", equalTo(id))
            .body("email", equalTo(email))
            .body("name.displayName", equalTo("Holly Cummins"));
    }

    @Test
    void renameEndpointUpdatesName() {
        String id = registerPerson(nextHandle());

        given()
            .contentType("application/json")
            .body("{\"name\":{\"first\":\"James\",\"last\":\"Cummins\"}}")
            .when().put("/api/people/" + id + "/name")
            .then().statusCode(200)
            .body("name.first", equalTo("James"))
            .body("name.last", equalTo("Cummins"))
            .body("name.displayName", equalTo("James Cummins"));
    }

    @Test
    void updateBioEndpointUpdatesBio() {
        String id = registerPerson(nextHandle());
        String newBio = "B".repeat(60);

        given()
            .contentType("application/json")
            .body("{\"text\":\"" + newBio + "\"}")
            .when().put("/api/people/" + id + "/bio")
            .then().statusCode(200)
            .body("bio", equalTo(newBio));
    }

    @Test
    void updateSocialsEndpointSetsAndClears() {
        String id = registerPerson(nextHandle());

        // Set socials
        given()
            .contentType("application/json")
            .body("{\"twitter\":\"https://twitter.com/holly\"," +
                  "\"linkedin\":\"https://linkedin.com/in/holly\"," +
                  "\"website\":\"https://holly.dev\"}")
            .when().put("/api/people/" + id + "/socials")
            .then().statusCode(200)
            .body("socials.twitter", equalTo("https://twitter.com/holly"))
            .body("socials.linkedin", equalTo("https://linkedin.com/in/holly"))
            .body("socials.website", equalTo("https://holly.dev"));

        // Clear all socials (PUT with all nulls)
        given()
            .contentType("application/json")
            .body("{\"twitter\":null,\"linkedin\":null,\"website\":null}")
            .when().put("/api/people/" + id + "/socials")
            .then().statusCode(200)
            .body("socials.twitter", equalTo(null))
            .body("socials.linkedin", equalTo(null))
            .body("socials.website", equalTo(null));
    }

    @Test
    void getReturns404WhenPersonMissing() {
        given().when().get("/api/people/" + UUID.randomUUID())
            .then().statusCode(404)
            .body("error", equalTo("PersonNotFound"));
    }

    @Test
    void registerReturns400WhenEmailMalformed() {
        given()
            .contentType("application/json")
            .body("{\"name\":{\"first\":\"Holly\",\"last\":\"Cummins\"}," +
                  "\"email\":\"not-an-email\"," +
                  "\"bio\":\"" + VALID_BIO + "\"}")
            .when().post("/api/people")
            .then().statusCode(400)
            .body("error", equalTo("IllegalArgumentException"));
    }

    @Test
    void registerReturns400WhenBioTooShort() {
        given()
            .contentType("application/json")
            .body("{\"name\":{\"first\":\"Holly\",\"last\":\"Cummins\"}," +
                  "\"email\":\"" + nextHandle() + "\"," +
                  "\"bio\":\"Too short\"}")
            .when().post("/api/people")
            .then().statusCode(400)
            .body("error", equalTo("IllegalArgumentException"));
    }

    @Test
    void getAllPaginates() {
        // Register 3 people
        registerPerson(nextHandle());
        registerPerson(nextHandle());
        registerPerson(nextHandle());

        // Request page 0 with size 2 — must return exactly 2
        given()
            .queryParam("page", "0")
            .queryParam("size", "2")
            .when().get("/api/people")
            .then().statusCode(200)
            .body("$", hasSize(2));
    }
}
