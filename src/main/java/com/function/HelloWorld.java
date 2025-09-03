package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpToolProperty;
import com.microsoft.azure.functions.annotation.McpToolTrigger;

/**
 * Demonstrates a simple Azure Function that prints a provided string and logs "Hello, World!".
 * This function is triggered by an MCP Tool Trigger, which provides the input JSON.
 */
public class HelloWorld {
    /**
     * Azure function that:
     * <ul>
     *   <li>Logs the {@code triggerInput} provided by the MCP tool.</li>
     *   <li>Logs "Hello, World!" to demonstrate a simple response.</li>
     * </ul>
     *
     * @param triggerInput The JSON argument provided by the MCP tool. Extracted as a string.
     * @param context      The execution context for logging and tracing function execution.
     */
    @FunctionName("HelloWorld")
    public void logCustomTriggerInput(
            @McpToolTrigger(
                    name = "helloWorld",
                    description = "Gets code snippets from your snippet collection.")
            String toolArguments,
            @McpToolProperty(
                name = "message",
                propertyType = "string",
                description = "The message to be logged.",
                required = true)
            String message,
            final ExecutionContext context
    ) {
        context.getLogger().info("Hello, World!");
        // Log a simple message
        context.getLogger().info("Message:");
        context.getLogger().info(message);
    }
}
