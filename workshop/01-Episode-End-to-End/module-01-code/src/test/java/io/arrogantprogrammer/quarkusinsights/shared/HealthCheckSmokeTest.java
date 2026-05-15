package io.arrogantprogrammer.quarkusinsights.shared;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Smoke test that confirms the starter project boots end-to-end:
 * Quarkus starts, Dev Services brings up PostgreSQL, and the SmallRye
 * Health endpoint reports {@code UP}. If this passes on a fresh clone,
 * your environment is ready for Step 1.
 */
@QuarkusTest
class HealthCheckSmokeTest {

    @Test
    void healthEndpointReportsUp() {
        given()
            .when().get("/q/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
