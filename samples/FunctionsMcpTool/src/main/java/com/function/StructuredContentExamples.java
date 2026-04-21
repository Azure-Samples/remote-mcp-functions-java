package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpToolProperty;
import com.microsoft.azure.functions.annotation.McpToolTrigger;
import com.microsoft.azure.functions.mcp.McpContent;
import com.microsoft.azure.functions.mcp.McpToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates structured content support in Azure Functions MCP tools.
 * 
 * <p>These examples show three approaches for returning rich content from MCP tools:</p>
 * <ol>
 *   <li>{@link #getSnippetStructured} — {@code @McpContent}-annotated POJO (automatic text + structured content)</li>
 *   <li>{@link #renderImage} — Single content block (ImageContentBlock)</li>
 *   <li>{@link #getMultiContent} — Multiple content blocks (List of ContentBlock)</li>
 * </ol>
 * 
 * <p>Note: These functions require the {@code azure-functions-java-mcp} dependency which provides
 * the middleware that wraps return values into the MCP result envelope.</p>
 */
public class StructuredContentExamples {

    // ========================================================================
    // Example 1: @McpContent-annotated POJO
    // The middleware automatically creates both text content (for backwards
    // compatibility) and structured content (for clients that support it).
    // ========================================================================

    /**
     * A snippet POJO decorated with @McpContent. When returned from an MCP tool function,
     * this will be serialized as both text content (JSON text) and structured content
     * (JSON object), enabling MCP clients to parse the result programmatically.
     */
    @McpContent
    public static class SnippetResult {
        private String name;
        private String content;
        private String language;

        public SnippetResult() {
        }

        public SnippetResult(String name, String content, String language) {
            this.name = name;
            this.content = content;
            this.language = language;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    /**
     * Returns a code snippet as structured content. The {@code @McpContent} annotation
     * on {@code SnippetResult} tells the middleware to serialize the return value as both
     * text content and structured content.
     *
     * <p>MCP clients that support structured content can parse the JSON object directly.
     * Older clients will see the JSON as a text string.</p>
     */
    @FunctionName("GetSnippetStructured")
    public SnippetResult getSnippetStructured(
            @McpToolTrigger(
                    name = "getSnippetStructured",
                    description = "Gets a code snippet with structured content support.")
            String context,
            @McpToolProperty(
                    name = "snippetName",
                    propertyType = "string",
                    description = "The name of the snippet to retrieve.",
                    isRequired = true)
            String snippetName,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("Getting structured snippet: " + snippetName);

        return new SnippetResult(
                snippetName,
                "public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello!\"); } }",
                "java"
        );
    }

    // ========================================================================
    // Example 2: Single content block (ImageContentBlock)
    // For returning rich content types like images.
    // ========================================================================

    /**
     * Returns an image as a single content block. The middleware wraps this
     * in an MCP result envelope automatically.
     */
    @FunctionName("RenderImage")
    public ImageContent renderImage(
            @McpToolTrigger(
                    name = "renderImage",
                    description = "Returns a base64-encoded image.")
            String context,
            @McpToolProperty(
                    name = "data",
                    propertyType = "string",
                    description = "Base64-encoded image data.",
                    isRequired = true)
            String data,
            @McpToolProperty(
                    name = "mimeType",
                    propertyType = "string",
                    description = "MIME type of the image (e.g., image/png).")
            String mimeType,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("Rendering image with MIME type: " + mimeType);

        return new ImageContent(null, data, mimeType != null ? mimeType : "image/png");
    }

    // ========================================================================
    // Example 3: Multiple content blocks
    // For returning a list of mixed content types in a single response.
    // ========================================================================

    /**
     * Returns multiple content blocks — a text description followed by an image.
     * The middleware wraps this as a multi-content result.
     */
    @FunctionName("GetMultiContent")
    public List<Content> getMultiContent(
            @McpToolTrigger(
                    name = "getMultiContent",
                    description = "Returns multiple content blocks including text and an image.")
            String context,
            @McpToolProperty(
                    name = "imageData",
                    propertyType = "string",
                    description = "Base64-encoded image data.",
                    isRequired = true)
            String imageData,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("Generating multi-content response");

        return Arrays.asList(
                new TextContent("Here is the requested image:"),
                new ImageContent(null, imageData, "image/png")
        );
    }
}
