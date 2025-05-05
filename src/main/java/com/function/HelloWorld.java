package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpToolTrigger;

/**
 * Demonstrates a simple Azure Function that prints a provided string and logs "Hello, World!".
 * This function is triggered by an MCP Tool Trigger, which provides the input JSON.
 */
public class HelloWorld {
    /**
     * The JSON schema describing the arguments expected by the "getsnippets" tool.
     * In this example, it expects one property named "triggerInput" which is a string.
     */
    public static final String ARGUMENTS = """
        [
           {
              "propertyName": "triggerInput",
              "propertyType": "string",
              "description": "input string"
           }
        ]
    """;

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
                    toolName = "helloWorld",
                    description = "Gets code snippets from your snippet collection.",
                    toolProperties = ARGUMENTS
            )
            String triggerInput,
            final ExecutionContext context
    ) {
        // Log the input passed in from the MCP tool
        context.getLogger().info(triggerInput);

        // Log a simple message
        context.getLogger().info("Hello, World!");
    }
}
