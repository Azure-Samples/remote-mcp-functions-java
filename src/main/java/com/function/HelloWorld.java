package com.function;

import com.function.model.McpToolInvocationContext;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpToolProperty;
import com.microsoft.azure.functions.annotation.McpToolTrigger;

/**
 * Demonstrates a simple Azure Function that prints a provided string and logs "Hello, World!".
 * This function is triggered by an MCP Tool Trigger, which automatically deserializes JSON into a generic POJO.
 */
public class HelloWorld {
    /**
     * Azure function that:
     * <ul>
     *   <li>Receives the MCP tool invocation context automatically deserialized into a generic POJO.</li>
     *   <li>Logs "Hello, World!" to demonstrate a simple response.</li>
     *   <li>Logs the provided message parameter using flexible argument access.</li>
     * </ul>
     *
     * @param mcpToolInvocationContext The MCP tool invocation context automatically deserialized from JSON into a generic structure.
     * @param message The message to be logged, provided as an MCP tool property.
     * @param functionExecutionContext The execution context for logging and tracing function execution.
     */
    @FunctionName("HelloWorld")
    public String logCustomTriggerInput(
            @McpToolTrigger(
                    name = "helloWorld",
                    description = "Says hello and logs the message that is provided.")
            McpToolInvocationContext mcpToolInvocationContext,
            @McpToolProperty(
                name = "message",
                propertyType = "string",
                description = "The message to be logged.",
                required = true)
            String message,
            final ExecutionContext functionExecutionContext
    ) {
        functionExecutionContext.getLogger().info("Hello, World!");
        functionExecutionContext.getLogger().info("Tool Name: " + mcpToolInvocationContext.getName());
        functionExecutionContext.getLogger().info("Progress Token: " + mcpToolInvocationContext.getMeta().get("progressToken").getAsInt());
        // Access arguments using direct JsonObject access
        functionExecutionContext.getLogger().info("Message from POJO: " + mcpToolInvocationContext.getArguments().get("message").getAsString());
        // Also log the message from the MCP property
        functionExecutionContext.getLogger().info("Message from MCP Property: " + message);
        
        return "Hello! I received and processed your message: '" + message + "'";
    }
}
