package io.arrogantprogrammer.quarkusinsights.admin.interfaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * End-to-end tests for the admin People pages.
 */
@QuarkusTest
class PersonAdminControllerTest {

    @Test
    void listPageRenders() {
        given().when().get("/admin/people")
            .then().statusCode(200)
            .body(containsString("People"))
            .body(containsString("Register person"));
    }

    @Test
    void newPageRendersForm() {
        given().when().get("/admin/people/new")
            .then().statusCode(200)
            .body(containsString("First name"))
            .body(containsString("Last name"))
            .body(containsString("Biography"));
    }

    @Test
    void registerFormSubmitRedirectsToDetail() {
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("firstName", "Admin")
            .formParam("lastName", "FormTest" + System.nanoTime())
            .formParam("email", "admin.formtest@example.com")
            .formParam("bio", "x".repeat(60))
            .redirects().follow(false)
            .when().post("/admin/people/new")
            .then().statusCode(303)
            .header("Location", matchesPattern(".*/admin/people/[0-9a-f\\-]{36}"));
    }

    @Test
    void registerFormReturns400OnShortBio() {
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("firstName", "Too")
            .formParam("lastName", "Short")
            .formParam("email", "shortbio@example.com")
            .formParam("bio", "too short")
            .redirects().follow(false)
            .when().post("/admin/people/new")
            .then().statusCode(400)
            .body(containsString("Bio"));
    }

    @Test
    void searchFragmentReturnsHtml() {
        given()
            .queryParam("search", "ZZZNoMatch" + System.nanoTime())
            .when().get("/admin/fragments/people-search")
            .then().statusCode(200)
            .body(containsString("No matches"));
    }
}
