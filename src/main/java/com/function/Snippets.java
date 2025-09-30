package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;

import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpToolProperty;
import com.microsoft.azure.functions.annotation.McpToolTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;

/**
 * This class contains two Azure Functions that demonstrate saving and retrieving text snippets
 * from Azure Blob storage, triggered by an MCP Tool Trigger annotation.
 */
public class Snippets {
    /**
     * The property name for the snippet's name in the JSON input.
     */
    public static final String SNIPPET_NAME_PROPERTY_NAME = "snippetName";

    /**
     * The property name for the snippet's content in the JSON input.
     */
    public static final String SNIPPET_PROPERTY_NAME = "snippet";

    /**
     * The path for the snippet blob in Azure Storage. The path
     * uses the snippet name property from the JSON input to uniquely store or retrieve files.
     *
     * Example final path: "snippets/myTestSnippet.json"
     */
    public static final String BLOB_PATH =
            "snippets/{mcptoolargs." + SNIPPET_NAME_PROPERTY_NAME + "}.json";

    /**
     * Azure Function that handles saving a text snippet to Azure Blob Storage.
     * <p>
     * The function is triggered via an MCP Tool Trigger. The snippet name and content
     * are provided as MCP tool properties, and the snippet content is saved to a blob
     * at a path derived from the snippet name.
     *
     * @param mcpToolInvocationContext The JSON input from the MCP tool trigger.
     * @param snippetName   The name of the snippet, provided as an MCP tool property.
     * @param snippet       The content of the snippet, provided as an MCP tool property.
     * @param outputBlob    The Azure Blob output binding where the snippet content is stored.
     * @param functionExecutionContext       The execution context for logging.
     */
    @FunctionName("SaveSnippets")
    @StorageAccount("AzureWebJobsStorage")
    public String saveSnippet(
            @McpToolTrigger(
                    name = "saveSnippets",
                    description = "Saves a text snippet to your snippets collection.")
            String mcpToolInvocationContext,
            @McpToolProperty(
                name = SNIPPET_NAME_PROPERTY_NAME,
                propertyType = "string",
                description = "The name of the snippet.",
                required = true)
            String snippetName,
            @McpToolProperty(
                name = SNIPPET_PROPERTY_NAME,
                propertyType = "string",
                description = "The content of the snippet.",
                required = true)
            String snippet,
            @BlobOutput(name = "outputBlob", path = BLOB_PATH)
            OutputBinding<String> outputBlob,
            final ExecutionContext functionExecutionContext
    ) {
        // Log the entire incoming JSON for debugging
        functionExecutionContext.getLogger().info(mcpToolInvocationContext);

        // Log the snippet name and content
        functionExecutionContext.getLogger().info("Saving snippet with name: " + snippetName);
        functionExecutionContext.getLogger().info("Snippet content:\n" + snippet);

        // Write the snippet content to the output blob
        outputBlob.setValue(snippet);
        
        return "Successfully saved snippet '" + snippetName + "' with " + snippet.length() + " characters.";
    }

    /**
     * Azure Function that handles retrieving a text snippet from Azure Blob Storage.
     * <p>
     * The function is triggered by an MCP Tool Trigger. The snippet name is provided
     * as an MCP tool property, and the snippet content is read from the blob at the 
     * path derived from the snippet name.
     *
     * @param mcpToolInvocationContext The JSON input from the MCP tool trigger.
     * @param snippetName   The name of the snippet to retrieve, provided as an MCP tool property.
     * @param inputBlob     The Azure Blob input binding that fetches the snippet content.
     * @param functionExecutionContext       The execution context for logging.
     */
    @FunctionName("GetSnippets")
    @StorageAccount("AzureWebJobsStorage")
    public String getSnippet(
            @McpToolTrigger(
                name = "getSnippets",
                description = "Gets a text snippet from your snippets collection.")
            String mcpToolInvocationContext,
            @McpToolProperty(
                name = SNIPPET_NAME_PROPERTY_NAME,
                propertyType = "string",
                description = "The name of the snippet.",
                required = true)
            String snippetName,
            @BlobInput(name = "inputBlob", path = BLOB_PATH)
            String inputBlob,
            final ExecutionContext functionExecutionContext
    ) {
        // Log the entire incoming JSON for debugging
        functionExecutionContext.getLogger().info(mcpToolInvocationContext);

        // Log the snippet name and the fetched snippet content from the blob
        functionExecutionContext.getLogger().info("Retrieving snippet with name: " + snippetName);
        functionExecutionContext.getLogger().info("Snippet content:");
        functionExecutionContext.getLogger().info(inputBlob);
        
        // Return the snippet content or a not found message
        if (inputBlob != null && !inputBlob.trim().isEmpty()) {
            return inputBlob;
        } else {
            return "Snippet '" + snippetName + "' not found.";
        }
    }
}
