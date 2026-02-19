package com.metar;

public class WeatherReportDto {

    public String airportCode;
    public String rawMetar;
    public String friendlyReport;
    public String error;

    public WeatherReportDto() {
    }

    public WeatherReportDto(String airportCode, String rawMetar, String friendlyReport) {
        this.airportCode = airportCode;
        this.rawMetar = rawMetar;
        this.friendlyReport = friendlyReport;
    }

    public static WeatherReportDto error(String airportCode, String message) {
        WeatherReportDto dto = new WeatherReportDto();
        dto.airportCode = airportCode;
        dto.error = message;
        return dto;
    }
}
