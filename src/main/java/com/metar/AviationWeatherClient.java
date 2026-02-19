package com.metar;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST client for the Aviation Weather Center API.
 * <p>
 * Fetches raw METAR (Meteorological Aerodrome Report) strings from
 * <a href="https://aviationweather.gov">aviationweather.gov</a>.
 * The base URL and timeouts are configured in {@code application.properties}
 * under the {@code aviation-weather} config key.
 */
@Path("")
@RegisterRestClient(configKey = "aviation-weather")
public interface AviationWeatherClient {

    /**
     * Retrieves the latest METAR observation for the given airport.
     *
     * @param airportCode the ICAO airport identifier (e.g. {@code KJFK}, {@code EGLL})
     * @return the raw METAR string, or an empty string if no data is available
     */
    @GET
    @Path("/api/data/metar")
    @Produces(MediaType.TEXT_PLAIN)
    String getMetar(@QueryParam("ids") String airportCode);
}
