package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpMetadata;
import com.microsoft.azure.functions.annotation.McpResourceTrigger;

/**
 * This class contains Azure Functions that demonstrate MCP Resource Triggers,
 * exposing content as MCP resources that AI assistants can discover and read.
 */
public class Resources {

    /**
     * Azure Function that exposes a text resource via MCP, with metadata.
     * <p>
     * When an MCP client requests the "file://readme.md" resource,
     * this function returns the readme content as plain text.
     * The {@code @McpMetadata} annotation attaches arbitrary JSON that is
     * surfaced in the MCP protocol's {@code _meta} field on {@code resources/list}.
     *
     * @param context                  The resource invocation context JSON from the MCP trigger.
     * @param functionExecutionContext The execution context for logging.
     * @return The text content of the resource.
     */
    @FunctionName("GetReadmeResource")
    public String getReadmeResource(
            @McpResourceTrigger(
                    name = "context",
                    uri = "file://readme.md",
                    resourceName = "readme",
                    description = "Application readme file with usage instructions",
                    mimeType = "text/plain")
            @McpMetadata(
                    name = "context",
                    json = "{\"author\": \"John Doe\", \"file\": {\"version\": 1.0, \"releaseDate\": \"2024-01-01\"}}")
            String context,
            final ExecutionContext functionExecutionContext) {

        functionExecutionContext.getLogger().info("MCP Resource trigger fired for readme resource");
        functionExecutionContext.getLogger().info("Invocation context: " + context);

        return "# Snippets MCP Application\n\n"
                + "This application provides MCP tools for saving and retrieving text snippets.\n\n"
                + "## Available Tools\n"
                + "- **SaveSnippets** - Save a text snippet with a name\n"
                + "- **GetSnippets** - Retrieve a saved snippet by name\n\n"
                + "## Available Resources\n"
                + "- **readme** (file://readme.md) - This readme file\n";
    }
}
