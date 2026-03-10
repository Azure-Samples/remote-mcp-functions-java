package com.function.weather;

/**
 * Represents a weather lookup error.
 */
public class WeatherError {

    private final String location;
    private final String error;
    private final String source;

    public WeatherError(String location, String error, String source) {
        this.location = location;
        this.error = error;
        this.source = source;
    }

    public String getLocation() { return location; }
    public String getError() { return error; }
    public String getSource() { return source; }
}
