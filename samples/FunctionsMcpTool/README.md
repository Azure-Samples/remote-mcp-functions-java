# FunctionsMcpTool Sample

A sample MCP server built with Azure Functions (Java) that demonstrates saving and retrieving code snippets using Azure Blob Storage.

## What It Does

This sample exposes three MCP tools:

| Tool | Description |
|------|-------------|
| **HelloWorld** | Says hello and logs the messages you provide. |
| **SaveSnippets** | Saves a text snippet to Azure Blob Storage. |
| **GetSnippets** | Retrieves a previously saved snippet by name. |

## Prerequisites

- [JDK 17](https://learn.microsoft.com/java/openjdk/download) (or newer)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local) >= `4.0.7030`
- [Docker](https://www.docker.com/) (for Azurite storage emulator)

## Getting Started

### 1. Start the Storage Emulator

The snippet tools persist data in Azure Blob Storage. For local development, start Azurite:

```bash
docker run -d -p 10000:10000 -p 10001:10001 -p 10002:10002 \
    mcr.microsoft.com/azure-storage/azurite
```

Or use the Azurite VS Code extension and run **Azurite: Start**.

### 2. Build and Run

```bash
cd samples/FunctionsMcpTool
mvn clean package
mvn azure-functions:run
```

The MCP endpoint will be available at `http://localhost:7071/runtime/webhooks/mcp`.

### 3. Connect from VS Code

Open **.vscode/mcp.json** at the repo root. Find the server called *local-mcp-function* and click **Start** above the name.

### 4. Try the Tools

In Copilot chat (agent mode), try prompts like:

```text
Say Hello
Save this snippet as snippet1
Retrieve snippet1 and apply to MyFile.java
```

## Source Code

| File | Description |
|------|-------------|
| [HelloWorld.java](src/main/java/com/function/HelloWorld.java) | Simple tool that logs messages and says hello. |
| [Snippets.java](src/main/java/com/function/Snippets.java) | Save and retrieve snippets using `@BlobOutput` / `@BlobInput` bindings. |
| [McpToolInvocationContext.java](src/main/java/com/function/model/McpToolInvocationContext.java) | POJO for the MCP tool invocation context. |

### Key Concepts

**MCP Tool Trigger** — The `@McpToolTrigger` annotation exposes a Java function as an MCP tool:

```java
@FunctionName("SaveSnippets")
@StorageAccount("AzureWebJobsStorage")
public String saveSnippet(
        @McpToolTrigger(
                name = "saveSnippets",
                description = "Saves a text snippet to your snippets collection.")
        String mcpToolInvocationContext,
        @McpToolProperty(name = "snippetName", propertyType = "string",
                description = "The name of the snippet.", isRequired = true)
        String snippetName,
        @McpToolProperty(name = "snippet", propertyType = "string",
                description = "The content of the snippet.", isRequired = true)
        String snippet,
        @BlobOutput(name = "outputBlob", path = BLOB_PATH)
        OutputBinding<String> outputBlob,
        final ExecutionContext functionExecutionContext) {
    outputBlob.setValue(snippet);
    return "Successfully saved snippet '" + snippetName + "'";
}
```

**MCP Tool Properties** — Use `@McpToolProperty` to declare input parameters that the MCP client passes to your tool.

**Azure Bindings** — Standard Azure Functions bindings (`@BlobInput`, `@BlobOutput`, `@StorageAccount`) work seamlessly with MCP triggers.
