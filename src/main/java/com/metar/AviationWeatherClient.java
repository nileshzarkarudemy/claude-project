package com.metar;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient(configKey = "aviation-weather")
public interface AviationWeatherClient {

    @GET
    @Path("/api/data/metar")
    @Produces(MediaType.TEXT_PLAIN)
    String getMetar(@QueryParam("ids") String airportCode);
}
