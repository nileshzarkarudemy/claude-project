package com.metar;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * JAX-RS resource exposing the METAR weather decoding API.
 * <p>
 * Provides a single endpoint that accepts an ICAO airport code, fetches the
 * latest METAR observation, and returns an AI-decoded plain-English summary
 * alongside the raw METAR string.
 */
@Path("/api")
public class MetarResource {

    @Inject
    MetarService metarService;

    /**
     * Returns a plain-English weather report for the given airport.
     * <p>
     * The airport code must be a 3–4 character ICAO identifier (e.g. {@code KJFK},
     * {@code EGLL}, {@code YSSY}). The response includes the raw METAR string and an
     * AI-generated friendly summary.
     *
     * @param airport the ICAO airport code supplied as a query parameter
     * @return {@code 200 OK} with a {@link WeatherReportDto} on success,
     *         {@code 400 Bad Request} if the code is missing or malformed,
     *         {@code 404 Not Found} if no METAR data could be retrieved
     */
    @GET
    @Path("/weather")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public Response getWeather(@QueryParam("airport") String airport) {
        if (airport == null || airport.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(WeatherReportDto.error("", "Airport code is required."))
                    .build();
        }

        String code = airport.trim().toUpperCase();
        if (!code.matches("[A-Z0-9]{3,4}")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(WeatherReportDto.error(code,
                            "Invalid airport code format. Use a 3-4 character ICAO code like KJFK or EGLL."))
                    .build();
        }

        WeatherReportDto result = metarService.getWeatherReport(code);

        if (result.error != null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(result)
                    .build();
        }

        return Response.ok(result).build();
    }
}
