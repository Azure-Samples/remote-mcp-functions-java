# FunctionsMcpTool Sample

A sample MCP server built with Azure Functions (Java) that demonstrates various MCP tools.

## What It Does

This sample exposes three MCP tools:

| Tool | Description |
| --- | --- |
| **HelloWorld** | Says hello and logs the messages you provide. |
| **SaveSnippets** | Saves a text snippet to Azure Blob Storage. |
| **GetSnippets** | Retrieves a previously saved snippet by name. |

## Prerequisites

- [JDK 17](https://learn.microsoft.com/java/openjdk/download) (or newer)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local) >= `4.0.7030`
- [Docker](https://www.docker.com/) (for Azurite storage emulator)

> [!NOTE]
> This sample uses the **Preview** extension bundle (`Microsoft.Azure.Functions.ExtensionBundle.Preview`) because MCP prompt bindings (`mcpPromptTrigger`, `mcpPromptArgument`) are not yet available in the standard bundle. See `host.json` for the configuration.

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

## Deploy to Azure

### 1. Sign in and create an environment

```bash
azd auth login

# This also becomes the resource group name
azd env new <environment-name>
```

### 2. Configure Entra authentication

Pre-authorize VS Code to request access tokens from Microsoft Entra:

```bash
azd env set PRE_AUTHORIZED_CLIENT_IDS aebc6443-996d-45c2-90f0-388ff96faa56
```

**Optional:** Enable VNet isolation:

```bash
azd env set VNET_ENABLED true
```

### 3. Deploy

```bash
azd up
```

### 4. Connect to the remote MCP server

After deployment, open **.vscode/mcp.json** and click **Start** above *remote-mcp-function*. Enter the `functionapp-name` from your azd output (or `/.azure/*/.env`). You'll be prompted to authenticate with Microsoft — click **Allow** and sign in with your Azure subscription email.

> [!TIP]
> A successful connection shows the number of tools the server has. Click **More... -> Show Output** for details on the VS Code ↔ server interactions.

## Redeploy and clean up

**Redeploy:** Run `azd up` as many times as needed to deploy code updates.

**Clean up:** Delete all Azure resources when done:

```shell
azd down
```

## Source Code

| File | Description |
| --- | --- |
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

## Troubleshooting

| Problem | Solution |
| --- | --- |
| Connection refused locally | Ensure Azurite is running |
| API version not supported by Azurite | Pull the latest image: `docker pull mcr.microsoft.com/azure-storage/azurite` |
| `mvn clean package` fails | Ensure JDK 17+ is on your PATH (`java -version`) |
| `azd up` provision succeeded but deploy immediately failed: `unable to find a resource tagged with 'azd-service-name: mcp'` | The tag was provisioned but not propagated yet when `azd deploy` looked it up — run `azd deploy` again |
| `azd deploy` fails with Kudu restart error: `deployment was partially successful: [KuduSpecializer] Kudu has been restarted after package deployed` | Transient error — run `azd deploy` again |

### Image tools cause "Could not process image" errors in VS Code

When using tools that return image content (`renderImage`, `getMultiContent`), you may see:

```text
Request Failed: 400 {"message":"Could not process image"}
```

**What's happening:** The tools are returning content correctly — the function logic is working as expected. The image is returned and displayed successfully on the first response. However, on your *next* message, VS Code sends the full conversation history (including the image) back to the language model. The model endpoint may reject the image data in the history, causing the 400 error. This is a chat rendering/infrastructure issue, not a problem with the function app or MCP tool implementation.
