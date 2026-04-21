package com.function.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpMetadata;
import com.microsoft.azure.functions.annotation.McpResourceTrigger;
import com.microsoft.azure.functions.annotation.McpToolProperty;
import com.microsoft.azure.functions.annotation.McpToolTrigger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * MCP Weather App — demonstrates MCP Apps with Azure Functions.
 * <p>
 * Contains two functions:
 * <ul>
 *   <li>{@code GetWeather} — MCP tool that fetches weather data and declares a UI resource</li>
 *   <li>{@code GetWeatherWidget} — MCP resource that serves the bundled HTML UI</li>
 * </ul>
 */
public class WeatherFunction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TOOL_METADATA = """
            {
                "ui": {
                    "resourceUri": "ui://weather/index.html"
                }
            }
            """;

    private static final String RESOURCE_METADATA = """
            {
                "ui": {
                    "prefersBorder": true
                }
            }
            """;

    private final WeatherService weatherService = new WeatherService();

    /**
     * MCP Resource: serves the weather widget UI.
     * <p>
     * The bundled {@code index.html} from {@code app/dist/} is loaded from the
     * classpath (or filesystem) and returned as HTML content.
     */
    @FunctionName("GetWeatherWidget")
    public String getWeatherWidget(
            @McpResourceTrigger(
                    name = "context",
                    uri = "ui://weather/index.html",
                    resourceName = "Weather Widget",
                    title = "Weather Widget",
                    description = "Interactive weather display for MCP Apps",
                    mimeType = "text/html;profile=mcp-app")
            @McpMetadata(
                    name = "context",
                    json = RESOURCE_METADATA)
            String context,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("GetWeatherWidget: serving weather widget UI");

        // Try loading relative to the jar location (required on Azure where CWD differs)
        try {
            java.io.File jarDir = new java.io.File(
                    WeatherFunction.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            java.io.File jarRelative = new java.io.File(jarDir, "app/dist/index.html");
            if (jarRelative.exists()) {
                return java.nio.file.Files.readString(jarRelative.toPath(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            executionContext.getLogger().log(Level.WARNING, "Failed to resolve jar-relative path", e);
        }

        // Fallback: try CWD-relative path (works for local dev)
        java.io.File file = new java.io.File("app/dist/index.html");
        if (file.exists()) {
            try {
                return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                executionContext.getLogger().log(Level.WARNING, "Failed to read UI from file", e);
            }
        }

        // Fallback: try classpath resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("app/dist/index.html")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            executionContext.getLogger().log(Level.WARNING, "Failed to read UI from classpath", e);
        }

        return "<html><body><p>Weather widget UI not found. Run <code>npm run build</code> in the <code>app/</code> directory.</p></body></html>";
    }

    /**
     * MCP Tool: fetches current weather for a given location.
     * <p>
     * Returns structured weather data (JSON) that the MCP host can pass to the
     * UI resource declared in the tool metadata.
     */
    @FunctionName("GetWeather")
    public String getWeather(
            @McpToolTrigger(
                    name = "GetWeather",
                    description = "Returns current weather for a location via Open-Meteo.")
            @McpMetadata(
                    name = "GetWeather",
                    json = TOOL_METADATA)
            String context,
            @McpToolProperty(
                    name = "location",
                    propertyType = "string",
                    description = "City name to check weather for (e.g., Seattle, New York, Miami)")
            String location,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("GetWeather: looking up weather for '" + location + "'");

        Object result = weatherService.getCurrentWeather(location);

        if (result instanceof WeatherResult weather) {
            executionContext.getLogger().info("Weather fetched for " + weather.getLocation()
                    + ": " + weather.getTemperatureC() + "°C");
        } else if (result instanceof WeatherError error) {
            executionContext.getLogger().warning("Weather error for " + error.getLocation()
                    + ": " + error.getError());
        }

        try {
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            executionContext.getLogger().log(Level.SEVERE, "Failed to serialize weather result", e);
            return "{\"error\": \"Serialization failed\"}";
        }
    }
}
