package io.arrogantprogrammer.quarkusinsights.admin.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Verifies the admin dashboard renders at {@code GET /admin} with
 * the expected nav and headings.
 */
@QuarkusTest
class AdminDashboardControllerTest {

    @Test
    void rendersDashboardWithNav() {
        given().when().get("/admin")
            .then().statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("Quarkus Insights — Admin"))
            .body(containsString("Dashboard"))
            .body(containsString("Episodes"))
            .body(containsString("People"));
    }
}
