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
     * @param messages The message to be logged, provided as an MCP tool property.
     * @param functionExecutionContext The execution context for logging and tracing function execution.
     */
    @FunctionName("HelloWorld")
    public String logCustomTriggerInput(
            @McpToolTrigger(
                    name = "helloWorld",
                    description = "Says hello and logs the messages that are provided.")
            McpToolInvocationContext mcpToolInvocationContext,
            @McpToolProperty(
                name = "messages",
                propertyType = "string",
                description = "The messages to be logged.",
                isRequired = true,
                isArray = true)
            String messages,
            final ExecutionContext functionExecutionContext
    ) {
        functionExecutionContext.getLogger().info("Hello, World!");
        functionExecutionContext.getLogger().info("Tool Name: " + mcpToolInvocationContext.getName());
        functionExecutionContext.getLogger().info("Transport Type: " + mcpToolInvocationContext.getTransportType());
        
        // Handle different transport types
        if (mcpToolInvocationContext.isHttpStreamable()) {
            functionExecutionContext.getLogger().info("Session ID: " + mcpToolInvocationContext.getSessionid());
        } else if (mcpToolInvocationContext.isHttpSse()) {
            if (mcpToolInvocationContext.getClientinfo() != null) {
                functionExecutionContext.getLogger().info("Client: " + 
                    mcpToolInvocationContext.getClientinfo().get("name").getAsString() + " v" +
                    mcpToolInvocationContext.getClientinfo().get("version").getAsString());
            }
        }
        
        // Access arguments using direct JsonObject access
        functionExecutionContext.getLogger().info("Messages from POJO: " + mcpToolInvocationContext.getArguments().get("messages"));
        // Also log the message from the MCP property
        functionExecutionContext.getLogger().info("Messages from MCP Property: " + messages);
        
        return "Hello! I received and processed your messages: '" + messages + "' via " + mcpToolInvocationContext.getTransportType();
    }
}
