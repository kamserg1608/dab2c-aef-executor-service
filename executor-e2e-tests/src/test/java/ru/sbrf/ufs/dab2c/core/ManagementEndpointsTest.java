package ru.sbrf.ufs.dab2c.core;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ManagementEndpointsTest extends BasicIntegrationTest{

    @BeforeEach
    void setUp() {
        RestAssured.basePath = servletContext.getContextPath();
    }

    @Test
    void environmentProduct() {
        given()
                .when()
                    .get("/environment/product")
                .then()
                    .statusCode(200)
                    .body("success", is(true))
                    .body("body.subsystem", equalTo("SYBSYSTEM_CODE_PLACEHOLDER"));
    }

    @Test
    void actuatorHealth() {
        given()
                .when()
                    .get("/healthcheck")
                .then()
                    .statusCode(200);
    }

}
