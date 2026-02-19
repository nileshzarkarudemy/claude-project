package com.metar;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api")
public class MetarResource {

    @Inject
    MetarService metarService;

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
