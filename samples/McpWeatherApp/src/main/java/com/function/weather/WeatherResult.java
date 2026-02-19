package com.function.weather;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a successful weather observation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherResult {

    private final String location;
    private final String condition;
    private final Integer temperatureC;
    private final Integer temperatureF;
    private final Integer humidityPercent;
    private final Integer windKph;
    private final String wind;
    private final String reportedAtUtc;
    private final String source;

    public WeatherResult(String location, String condition, Integer temperatureC, Integer temperatureF,
                         Integer humidityPercent, Integer windKph, String wind, String reportedAtUtc, String source) {
        this.location = location;
        this.condition = condition;
        this.temperatureC = temperatureC;
        this.temperatureF = temperatureF;
        this.humidityPercent = humidityPercent;
        this.windKph = windKph;
        this.wind = wind;
        this.reportedAtUtc = reportedAtUtc;
        this.source = source;
    }

    public String getLocation() { return location; }
    public String getCondition() { return condition; }
    public Integer getTemperatureC() { return temperatureC; }
    public Integer getTemperatureF() { return temperatureF; }
    public Integer getHumidityPercent() { return humidityPercent; }
    public Integer getWindKph() { return windKph; }
    public String getWind() { return wind; }
    public String getReportedAtUtc() { return reportedAtUtc; }
    public String getSource() { return source; }
}
