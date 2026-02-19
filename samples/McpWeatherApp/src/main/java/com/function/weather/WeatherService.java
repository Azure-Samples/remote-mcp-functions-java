package com.function.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * Fetches current weather data from the Open-Meteo API.
 * <p>
 * Uses Open-Meteo geocoding (no API key required) to resolve city names to
 * coordinates, then fetches current weather observations.
 */
public class WeatherService {

    private static final String WEATHER_BASE = "https://api.open-meteo.com/";
    private static final String GEOCODE_BASE = "https://geocoding-api.open-meteo.com/";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public WeatherService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Look up current weather for the given location string.
     *
     * @param location a city name, address, or zip code
     * @return a {@link WeatherResult} on success or a {@link WeatherError} on failure
     */
    public Object getCurrentWeather(String location) {
        String normalized = normalizeLocation(location);

        try {
            String[] geocodeResult = geocodeWithName(normalized);
            if (geocodeResult == null) {
                return new WeatherError(normalized,
                        "Could not find this location. Try a city, address, or zip code.", "open-meteo");
            }

            double lat = Double.parseDouble(geocodeResult[0]);
            double lon = Double.parseDouble(geocodeResult[1]);
            String canonical = geocodeResult[2];

            JsonNode observation = getLatestObservation(lat, lon);
            if (observation == null) {
                return new WeatherError(canonical, "Could not retrieve current observations.", "open-meteo");
            }

            return parseObservation(observation, canonical);

        } catch (Exception ex) {
            return new WeatherError(normalized, "Unable to fetch weather: " + ex.getMessage(), "api.open-meteo.com");
        }
    }

    private static String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return "Seattle, WA";
        }
        return location.trim();
    }

    /**
     * Geocode a location string using the Open-Meteo geocoding API.
     *
     * @return [latitude, longitude, canonicalName] or null if not found
     */
    private String[] geocodeWithName(String location) throws Exception {
        String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);
        String url = GEOCODE_BASE + "v1/search?name=" + encoded + "&count=1&language=en&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(response.body());

        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            return null;
        }

        JsonNode match = results.get(0);
        double lat = match.get("latitude").asDouble();
        double lon = match.get("longitude").asDouble();

        String name = match.has("name") ? match.get("name").asText() : location;
        String admin1 = match.has("admin1") ? match.get("admin1").asText() : null;
        String country = match.has("country") ? match.get("country").asText() : null;

        StringJoiner canonical = new StringJoiner(", ");
        if (name != null && !name.isBlank()) canonical.add(name);
        if (admin1 != null && !admin1.isBlank()) canonical.add(admin1);
        if (country != null && !country.isBlank()) canonical.add(country);

        String canonicalName = canonical.length() > 0 ? canonical.toString() : location;

        return new String[]{String.valueOf(lat), String.valueOf(lon), canonicalName};
    }

    /**
     * Fetch the latest observation from Open-Meteo for the given coordinates.
     */
    private JsonNode getLatestObservation(double lat, double lon) throws Exception {
        String url = WEATHER_BASE + "v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,weather_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(response.body());

        return root.has("current") ? root.get("current") : null;
    }

    /**
     * Parse the Open-Meteo current-weather observation into a {@link WeatherResult}.
     */
    private static WeatherResult parseObservation(JsonNode current, String location) {
        Double tempC = getDouble(current, "temperature_2m");
        Integer tempCInt = tempC != null ? (int) Math.round(tempC) : null;
        Integer tempFInt = tempC != null ? (int) Math.round(tempC * 1.8 + 32) : null;

        String condition = current.has("weather_code") && current.get("weather_code").isNumber()
                ? mapWeatherCode(current.get("weather_code").asInt())
                : "Unknown";

        Double humidity = getDouble(current, "relative_humidity_2m");
        Integer humidityInt = humidity != null ? (int) Math.round(humidity) : null;

        Double windSpeed = getDouble(current, "wind_speed_10m");
        Integer windKph = windSpeed != null ? (int) Math.round(windSpeed) : null;

        Double windDirDeg = getDouble(current, "wind_direction_10m");
        String windDir = windDirDeg != null ? " " + degToCardinal(windDirDeg) : "";

        String windText = windKph != null ? windKph + " km/h" + windDir : "\u2014";

        String reportedTime;
        if (current.has("time") && current.get("time").isTextual()) {
            reportedTime = current.get("time").asText() + "Z";
        } else {
            reportedTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        }

        return new WeatherResult(
                location, condition, tempCInt, tempFInt,
                humidityInt, windKph, windText, reportedTime, "open-meteo"
        );
    }

    private static Double getDouble(JsonNode node, String field) {
        if (node.has(field) && node.get(field).isNumber()) {
            return node.get(field).asDouble();
        }
        return null;
    }

    private static String mapWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snowfall";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }

    private static String degToCardinal(double deg) {
        String[] dirs = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round((deg % 360) / 22.5) % 16;
        return dirs[index];
    }
}
