# Weather App Sample

A sample MCP App that displays weather information with an interactive UI.

## What Are MCP Apps?

[MCP Apps](https://blog.modelcontextprotocol.io/posts/2026-01-26-mcp-apps/) let tools return interactive interfaces instead of plain text. When a tool declares a UI resource, the host renders it in a sandboxed iframe where users can interact directly.

### MCP Apps = Tool + UI Resource

The architecture relies on two MCP primitives:

1. **Tools** with UI metadata pointing to a resource URI
2. **Resources** containing bundled HTML/JavaScript served via the `ui://` scheme

Azure Functions makes it easy to build both.

## Prerequisites

- [JDK 17](https://learn.microsoft.com/java/openjdk/download) (or newer)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Node.js](https://nodejs.org/) (for building the UI)
- [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local)
- An MCP-compatible host (VS Code with GitHub Copilot, Claude Desktop, etc.)

## Getting Started

### 1. Build the UI

The UI must be bundled before running the function app:

```bash
cd app
npm install
npm run build
cd ..
```

This creates a bundled `app/dist/index.html` file that the function serves.

### 2. Build and Run the Function App

```bash
mvn clean package
mvn azure-functions:run
```

The MCP server will be available at `http://localhost:7071/runtime/webhooks/mcp`.

### 3. Connect from VS Code

Open **.vscode/mcp.json** at the repo root. Find the server called *local-mcp-function* and click **Start** above the name.

### 4. Prompt the Agent

Ask Copilot: "What's the weather in Seattle?"

## Source Code

The source code is in [WeatherFunction.java](src/main/java/com/function/weather/WeatherFunction.java). The key concept is how **tools connect to resources via metadata**.

### The Tool with UI Metadata

The `GetWeather` tool uses `@McpMetadata` to declare it has an associated UI:

```java
private static final String TOOL_METADATA = """
        {
            "ui": {
                "resourceUri": "ui://weather/index.html"
            }
        }
        """;

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
    // Fetches weather data and returns JSON
    Object result = weatherService.getCurrentWeather(location);
    return MAPPER.writeValueAsString(result);
}
```

The `resourceUri` points to `ui://weather/index.html` — this tells the MCP host that when this tool is invoked, there's an interactive UI available at that resource URI.

### The Resource Serving the UI

The `GetWeatherWidget` function serves the bundled HTML at the matching URI:

```java
private static final String RESOURCE_METADATA = """
        {
            "ui": {
                "prefersBorder": true
            }
        }
        """;

@FunctionName("GetWeatherWidget")
public String getWeatherWidget(
        @McpResourceTrigger(
                name = "context",
                uri = "ui://weather/index.html",
                resourceName = "Weather Widget",
                description = "Interactive weather display for MCP Apps",
                mimeType = "text/html;profile=mcp-app")
        @McpMetadata(
                name = "context",
                json = RESOURCE_METADATA)
        String context,
        final ExecutionContext executionContext) {
    // Reads and returns app/dist/index.html
    return Files.readString(new File("app/dist/index.html").toPath());
}
```

### How It Works Together

1. User asks: "What's the weather in Seattle?"
2. Agent calls the `GetWeather` tool
3. Tool returns weather data (JSON) **and** the host sees the `ui.resourceUri` metadata
4. Host fetches the UI resource from `ui://weather/index.html`
5. Host renders the HTML in a sandboxed iframe, passing the tool result as context
6. User sees an interactive weather widget instead of plain text

### The UI (TypeScript)

The frontend in `app/src/weather-app.ts` receives the tool result and renders the weather display. It uses the `@modelcontextprotocol/ext-apps` SDK and is bundled with Vite into a single `index.html` that the resource serves.

## Project Structure

| Path | Description |
|------|-------------|
| [WeatherFunction.java](src/main/java/com/function/weather/WeatherFunction.java) | MCP tool + resource definitions |
| [WeatherService.java](src/main/java/com/function/weather/WeatherService.java) | Open-Meteo API client (geocoding + weather) |
| [WeatherResult.java](src/main/java/com/function/weather/WeatherResult.java) | Weather data POJO |
| [WeatherError.java](src/main/java/com/function/weather/WeatherError.java) | Error response POJO |
| [app/](app/) | Vite + TypeScript weather widget UI |
| [app/src/weather-app.ts](app/src/weather-app.ts) | Frontend entry point |
