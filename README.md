<!--
---
name: Remote MCP with Azure Functions (Java)
description: Run a remote MCP server on Azure Functions (Java).
languages:
- java
- bicep
- azdeveloper
products:
- azure-functions
- azure
page_type: sample
urlFragment: remote-mcp-functions-java
---
-->

# Getting Started with Remote MCP Servers using Azure Functions (Java)

This quick-start shows how to build and deploy a **remote MCP server** with Azure Functions (Java).  
You can run it locally for debugging, then ship it to the cloud with `azd up` in minutes.  
The server is secured by design (system keys + HTTPS), supports OAuth via EasyAuth or API Management, and can be isolated inside a VNet.

[Watch the overview video.](https://www.youtube.com/watch?v=U9DsLcP5vEk)
---

## Prerequisites

| Purpose | Tool | Notes |
|---------|------|-------|
| **Java build + run** | JDK 17 (or newer) | Java 8 runtime still works but JDK 17 is recommended. |
| **Local Functions runtime** | [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local) ≥ `4.0.7030` | Used by `mvn azure-functions:run`. |
| **Provision & deploy** | [Azure Developer CLI (azd)](https://aka.ms/azd) | Simplifies end-to-end deployment. |
| **IDE (optional)** | Visual Studio Code + [Azure Functions extension](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-azurefunctions) | One-click debug & log streaming. |
| **Blob Storage emulator** | [Docker](https://www.docker.com/) to run **Azurite** | Needed only when running locally with `UseDevelopmentStorage=true`. |

---

## Prepare your local environment

> **Why Azurite?**   
> The `SaveSnippet` / `GetSnippet` tools persist snippets in Azure Blob Storage.  
> Azurite emulates that storage account on your dev machine.

```bash
docker run -p 10000:10000 -p 10001:10001 -p 10002:10002 \
       mcr.microsoft.com/azure-storage/azurite
````

If you prefer the Azurite **VS Code extension**, run **“Azurite: Start”** from the command palette instead.

---

## Run the MCP server locally

```bash
# 1 – Build the project
mvn clean package

# 2 – Start the Functions host (via Maven wrapper)
mvn azure-functions:run
```

The SSE endpoint will be available at:

```
http://127.0.0.1:7071/runtime/webhooks/mcp/sse
```

---

## Try the *local* MCP server

### A. GitHub Copilot in VS Code

1. **Add MCP Server** → choose **HTTP (Server-Sent Events)**.

2. URL:

   ```text
   http://127.0.0.1:7071/runtime/webhooks/mcp/sse
   ```

3. Give it any server-ID you like and save to *User* or *Workspace* settings.

4. **List MCP Servers** → **Start** your new server.

5. In Copilot Chat (agent-mode) try prompts such as:

   ```text
   Say Hello
   Save this snippet as snippet1
   Retrieve snippet1 and apply to MyFile.java
   ```

6. When finished, **Stop** the server (command palette) and `Ctrl+C` the terminal running the function host.

### B. MCP Inspector

```bash
# In a second terminal
npx @modelcontextprotocol/inspector node build/index.js
# If the Functions host isn’t running, start it:
# mvn azure-functions:run
```

* Open the Inspector UI (URL printed in the terminal).
* Transport: **SSE**.
* URL: `http://127.0.0.1:7071/runtime/webhooks/mcp/sse` → **Connect**.
* **List Tools** → pick one → **Run Tool**.

Stop both terminals with `Ctrl+C` when done.

---

## Deploy to Azure (remote MCP)

Want the function app inside a VNet *before* provisioning? Just set:

```bash
azd env set VNET_ENABLED true
```

Then provision + deploy in one step:

```bash
azd up
```

---

## Connect clients to the *remote* MCP server

The hosted SSE endpoint will be:

```
https://<FUNC_APP_NAME>.azurewebsites.net/runtime/webhooks/mcp/sse
```

> **Key required**
> Grab the **system key** named `mcp_extension` from the Azure Portal
> or via CLI:
> `az functionapp keys list --resource-group <rg> --name <func-app>`

### MCP Inspector

```
https://<FUNC_APP_NAME>.azurewebsites.net/runtime/webhooks/mcp/sse?code=<mcp_extension_key>
```

### VS Code – GitHub Copilot

Add a header in `mcp.json`:

```jsonc
{
  "servers": {
    "remote-mcp-function": {
      "type": "sse",
      "url": "https://<FUNC_APP_NAME>.azurewebsites.net/runtime/webhooks/mcp/sse",
      "headers": {
        "x-functions-key": "<mcp_extension_key>"
      }
    },
    "local-mcp-function": {
      "type": "sse",
      "url": "http://127.0.0.1:7071/runtime/webhooks/mcp/sse"
    }
  }
}
```

Start **remote-mcp-function** and chat as usual in Copilot.

---

## Redeploy code

```bash
azd up   # Safe to run repeatedly—always overwrites the app with the latest build
```

## Clean up

```bash
azd down
```

---

## Source Code Layout

| Tool                           | Path                                                                                         | Description                                 |
| ------------------------------ | -------------------------------------------------------------------------------------------- | ------------------------------------------- |
| **HelloWorld**                 | [`src/main/java/com/function/HelloWorld.java`](./src/main/java/com/function/HelloWorld.java) | Logs an argument then prints “Hello World”. |
| **SaveSnippets / GetSnippets** | [`src/main/java/com/function/Snippets.java`](./src/main/java/com/function/Snippets.java)     | Saves / retrieves snippets to Blob Storage. |

---

## How It Works

<details>
<summary>Click to expand</summary>

### MCP Tool Trigger

```java
@McpToolTrigger(
    toolName    = "saveSnippets",
    description = "Saves a text snippet to your snippets collection.",
    toolProperties = SAVE_SNIPPET_ARGUMENTS   // JSON schema string
)
```

* `toolProperties` defines the JSON payload expected from the client.
* At runtime the JSON is passed to your function in `toolArguments`.

### Storage Bindings

```java
@BlobOutput(
    name = "snippetOut",
    path = "snippets/{mcptoolargs.snippetName}.json",
    dataType = "binary")
```

* `SaveSnippets` writes to a blob; `GetSnippets` reads the same path.
* The default storage account is referenced with `@StorageAccount("AzureWebJobsStorage")`.

### Required SDK + Extension Bundle

```xml
<azure.functions.java.library.version>3.1.1-alpha</azure.functions.java.library.version>
```

```jsonc
// host.json
{
  "extensionBundle": {
    "id": "Microsoft.Azure.Functions.ExtensionBundle.Experimental",
    "version": "[4.*, 5.0.0)"
  }
}
```

</details>

---

## Next Steps

* Front your MCP server with **API Management** for fine-grained policies.
* Add **EasyAuth** to use your favourite OAuth provider (including Entra ID).
* Toggle VNet integration via `VNET_ENABLED=true` for network isolation.
* Explore the [Model Context Protocol](https://github.com/modelcontextprotocol) ecosystem for more tools.