package com.metar;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class MetarServiceTest {

    private static final String KJFK_RAW =
            "KJFK 191551Z 27015KT 10SM FEW055 BKN250 12/M01 A2992 RMK AO2";
    private static final String EGLL_RAW =
            "EGLL 191550Z 22008KT 9999 BKN024 15/11 Q1013";
    private static final String DECODED_KJFK =
            "It's a cool, mostly clear afternoon at JFK with a moderate westerly breeze.";

    @Inject
    MetarService metarService;

    @InjectMock
    @RestClient
    AviationWeatherClient weatherClient;

    @InjectMock
    AiDecoder aiDecoder;

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test
    void shouldReturnDecodedReportForKjfkMetar() {
        when(weatherClient.getMetar("KJFK")).thenReturn(KJFK_RAW);
        when(aiDecoder.decode(KJFK_RAW)).thenReturn(DECODED_KJFK);

        WeatherReportDto result = metarService.getWeatherReport("KJFK");

        assertNull(result.error);
        assertEquals("KJFK",       result.airportCode);
        assertEquals(KJFK_RAW,     result.rawMetar);
        assertEquals(DECODED_KJFK, result.friendlyReport);
    }

    @Test
    void shouldPassRawMetarToAiDecoder() {
        when(weatherClient.getMetar("KJFK")).thenReturn(KJFK_RAW);
        when(aiDecoder.decode(KJFK_RAW)).thenReturn(DECODED_KJFK);

        metarService.getWeatherReport("KJFK");

        verify(aiDecoder).decode(KJFK_RAW);
    }

    @Test
    void shouldNormalizeInputAirportCodeToUppercase() {
        when(weatherClient.getMetar("EGLL")).thenReturn(EGLL_RAW);
        when(aiDecoder.decode(EGLL_RAW)).thenReturn("Mild and overcast at Heathrow.");

        WeatherReportDto result = metarService.getWeatherReport("egll");

        assertNull(result.error);
        assertEquals("EGLL",   result.airportCode);
        assertEquals(EGLL_RAW, result.rawMetar);
    }

    // ── Weather service failures ───────────────────────────────────────────────

    @Test
    void shouldReturnErrorDtoWhenWeatherClientThrowsException() {
        when(weatherClient.getMetar("KJFK"))
                .thenThrow(new RuntimeException("Connection timeout"));

        WeatherReportDto result = metarService.getWeatherReport("KJFK");

        assertNotNull(result.error);
        assertNull(result.rawMetar);
        assertNull(result.friendlyReport);
        assertTrue(result.error.contains("aviation weather service"));
    }

    @Test
    void shouldReturnErrorDtoWhenMetarResponseIsEmpty() {
        when(weatherClient.getMetar("ZZZZ")).thenReturn("");

        WeatherReportDto result = metarService.getWeatherReport("ZZZZ");

        assertNotNull(result.error);
        assertEquals("ZZZZ", result.airportCode);
        assertNull(result.rawMetar);
        assertNull(result.friendlyReport);
        assertTrue(result.error.contains("No METAR data found"));
    }

    // ── AI decoder failures ────────────────────────────────────────────────────

    @Test
    void shouldReturnErrorDtoWhenAiDecoderThrows() {
        when(weatherClient.getMetar("KJFK")).thenReturn(KJFK_RAW);
        when(aiDecoder.decode(any())).thenThrow(new RuntimeException("AI unavailable"));

        WeatherReportDto result = metarService.getWeatherReport("KJFK");

        assertNotNull(result.error);
        assertEquals("KJFK", result.airportCode);
        assertTrue(result.error.contains(KJFK_RAW));
    }
}
