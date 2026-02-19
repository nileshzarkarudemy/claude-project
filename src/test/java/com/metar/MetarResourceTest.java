package com.metar;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MetarResource}.
 * <p>
 * The full Quarkus HTTP server is started; {@link MetarService} is replaced with
 * a Mockito mock so these tests exercise only the REST layer — input validation,
 * HTTP status codes, and JSON response shape — without touching any external services.
 */
@QuarkusTest
class MetarResourceTest {

    @InjectMock
    MetarService metarService;

    private static final String RAW_METAR =
            "KJFK 191551Z 27015KT 10SM FEW055 BKN250 12/M01 A2992 RMK AO2";
    private static final String DECODED =
            "It's a cool, mostly clear afternoon at JFK with a moderate westerly breeze.";

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test
    void shouldReturn200WithFullPayloadForValidAirportCode() {
        when(metarService.getWeatherReport("KJFK"))
                .thenReturn(new WeatherReportDto("KJFK", RAW_METAR, DECODED));

        given()
            .queryParam("airport", "KJFK")
        .when()
            .get("/api/weather")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("airportCode",    equalTo("KJFK"))
            .body("rawMetar",       equalTo(RAW_METAR))
            .body("friendlyReport", equalTo(DECODED))
            .body("error",          nullValue());
    }

    @Test
    void shouldNormalizeLowercaseCodeToUppercaseBeforeCallingService() {
        // The resource uppercases the code before delegating, so the mock must
        // expect "KJFK", not "kjfk".
        when(metarService.getWeatherReport("KJFK"))
                .thenReturn(new WeatherReportDto("KJFK", RAW_METAR, DECODED));

        given()
            .queryParam("airport", "kjfk")
        .when()
            .get("/api/weather")
        .then()
            .statusCode(200)
            .body("airportCode", equalTo("KJFK"));
    }

    @Test
    void shouldAcceptThreeCharacterIcaoCode() {
        when(metarService.getWeatherReport("SYD"))
                .thenReturn(new WeatherReportDto("SYD", "SYD 191600Z ...", "Sunny in Sydney."));

        given()
            .queryParam("airport", "SYD")
        .when()
            .get("/api/weather")
        .then()
            .statusCode(200)
            .body("airportCode", equalTo("SYD"));
    }

    // ── Input validation — 400 Bad Request ────────────────────────────────────

    @Test
    void shouldReturn400WhenAirportParamIsAbsent() {
        given()
        .when()
            .get("/api/weather")
        .then()
            .statusCode(400)
            .body("error", notNullValue());
    }

    @Test
    void shouldReturn400WhenAirportParamIsBlank() {
        given()
            .queryParam("airport", "   ")
        .when()
            .get("/api/weather")
        .then()
            .statusCode(400)
            .body("error", notNullValue());
    }

    @Test
    void shouldReturn400WhenAirportCodeIsTooLong() {
        given()
            .queryParam("airport", "KJFKX")
        .when()
            .get("/api/weather")
        .then()
            .statusCode(400)
            .body("error", containsString("Invalid airport code format"));
    }

    @Test
    void shouldReturn400WhenAirportCodeContainsSpecialCharacters() {
        given()
            .queryParam("airport", "KJ!K")
        .when()
            .get("/api/weather")
        .then()
            .statusCode(400)
            .body("error", containsString("Invalid airport code format"));
    }

    // ── Service-level errors — 404 Not Found ──────────────────────────────────

    @Test
    void shouldReturn404WhenServiceReturnsAnErrorDto() {
        when(metarService.getWeatherReport("ZZZZ"))
                .thenReturn(WeatherReportDto.error("ZZZZ", "No METAR data found for airport code 'ZZZZ'."));

        given()
            .queryParam("airport", "ZZZZ")
        .when()
            .get("/api/weather")
        .then()
            .statusCode(404)
            .body("airportCode",    equalTo("ZZZZ"))
            .body("error",          containsString("No METAR data found"))
            .body("rawMetar",       nullValue())
            .body("friendlyReport", nullValue());
    }
}
