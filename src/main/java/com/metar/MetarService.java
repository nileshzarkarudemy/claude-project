package com.metar;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Application-scoped service that orchestrates METAR retrieval and AI decoding.
 * <p>
 * On each request it:
 * <ol>
 *   <li>Fetches the raw METAR string from the Aviation Weather Center API via
 *       {@link AviationWeatherClient}.</li>
 *   <li>Sends the raw METAR to the OpenAI GPT-4o model via {@link AiDecoder}
 *       asking for a plain-English weather summary.</li>
 *   <li>Returns the result wrapped in a {@link WeatherReportDto}.</li>
 * </ol>
 */
@ApplicationScoped
public class MetarService {

    private static final Logger LOG = Logger.getLogger(MetarService.class);

    @Inject
    @RestClient
    AviationWeatherClient weatherClient;

    @Inject
    AiDecoder aiDecoder;

    /**
     * Fetches and decodes the METAR report for the given airport code.
     *
     * @param airportCode a valid ICAO airport identifier (e.g. {@code KJFK})
     * @return a {@link WeatherReportDto} containing the raw METAR and decoded summary,
     *         or an error DTO if the weather service or AI decoding step fails
     */
    public WeatherReportDto getWeatherReport(String airportCode) {
        String code = airportCode.trim().toUpperCase();

        String rawMetar;
        try {
            rawMetar = weatherClient.getMetar(code).trim();
        } catch (Exception e) {
            LOG.errorf("Failed to fetch METAR for %s: %s", code, e.getMessage());
            return WeatherReportDto.error(code,
                    "Could not reach the aviation weather service. Please try again later.");
        }

        if (rawMetar.isEmpty()) {
            return WeatherReportDto.error(code,
                    "No METAR data found for airport code '" + code +
                    "'. Please verify this is a valid ICAO code (4 letters, e.g. KJFK, EGLL, YSSY).");
        }

        String friendlyReport;
        try {
            friendlyReport = aiDecoder.decode(rawMetar);
        } catch (Exception e) {
            LOG.error("OpenAI decoding failed for " + code, e);
            return WeatherReportDto.error(code,
                    "Retrieved the METAR but could not decode it (" +
                    e.getClass().getSimpleName() + ": " + e.getMessage() +
                    "). Raw data: " + rawMetar);
        }

        return new WeatherReportDto(code, rawMetar, friendlyReport);
    }
}
