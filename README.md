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

This is a quickstart template to easily build and deploy custom remote MCP servers to the cloud using Azure Functions. You can clone/restore/run on your local machine with debugging, and `azd up` to have it in the cloud in a couple minutes.

The MCP server is configured with [built-in authentication](https://learn.microsoft.com/en-us/azure/app-service/overview-authentication-authorization) using Microsoft Entra as the identity provider.

You can also use [API Management](https://learn.microsoft.com/azure/api-management/secure-mcp-servers) to secure the server, as well as network isolation using VNET.

If you're looking for this sample in more languages check out the [.NET/C#](https://github.com/Azure-Samples/remote-mcp-functions-dotnet), [Node.js/TypeScript](https://github.com/Azure-Samples/remote-mcp-functions-typescript) and [Python](https://github.com/Azure-Samples/remote-mcp-functions-python) samples.

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/Azure-Samples/remote-mcp-functions-java)

## Samples

| Sample | Description |
|--------|-------------|
| [**FunctionsMcpTool**](samples/FunctionsMcpTool/) | MCP tools with Azure Blob Storage - say hello, save and retrieve code snippets. |
| [**McpWeatherApp**](samples/McpWeatherApp/) | MCP Apps - a weather tool with an interactive UI (MCP resource + metadata). |

## Prerequisites

+ [JDK 17](https://learn.microsoft.com/java/openjdk/download) (or newer)
+ [Apache Maven](https://maven.apache.org/download.cgi)
+ [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local) >= `4.0.7030`
+ [Azure Developer CLI](https://aka.ms/azd) (for deployment)
+ [Visual Studio Code](https://code.visualstudio.com/) + [Azure Functions extension](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-azurefunctions) (recommended)

## Run and test locally

### FunctionsMcpTool

> **Requires [Azurite](https://learn.microsoft.com/azure/storage/common/storage-use-azurite)** for local blob storage. Start it with `docker run -d -p 10000:10000 -p 10001:10001 -p 10002:10002 mcr.microsoft.com/azure-storage/azurite` or use the Azurite VS Code extension.

```
cd samples/FunctionsMcpTool
mvn clean package
mvn azure-functions:run
```

Open **.vscode/mcp.json**, click **Start** above *local-mcp-function*, then try prompts in Copilot agent mode:

```
Say Hello
Save this snippet as snippet1
Retrieve snippet1
```

See [samples/FunctionsMcpTool/README.md](samples/FunctionsMcpTool/README.md) for full details including MCP Inspector setup.

### McpWeatherApp

```
cd samples/McpWeatherApp/app
npm install && npm run build
cd ..
mvn clean package
mvn azure-functions:run
```

Open **.vscode/mcp.json**, click **Start** above *local-mcp-function*, then ask Copilot: *"What's the weather in Seattle?"*

See [samples/McpWeatherApp/README.md](samples/McpWeatherApp/README.md) for details on MCP Apps architecture.

## Deploy to Azure

### Step 1: Sign in to Azure

```
az login
azd auth login
```

### Step 2: Create an environment and configure

```
azd env new <environment-name>
```

Configure VS Code as an allowed client application to request access tokens from Microsoft Entra (applies to the FunctionsMcpTool service):

```
azd env set PRE_AUTHORIZED_CLIENT_IDS aebc6443-996d-45c2-90f0-388ff96faa56
```

**Optional:** Enable VNet isolation:

```
azd env set VNET_ENABLED true
```

### Step 3: Deploy

Deploy both apps:

```
azd up
```

Or deploy a specific app:

```
# Deploy only the MCP Tool (with Entra auth)
azd deploy api

# Deploy only the Weather App (no auth required)
azd deploy weather
```

### Step 4: Connect to the remote MCP server

After deployment finishes, open **.vscode/mcp.json** and click **Start** above *remote-mcp-function*. You'll be prompted for `functionapp-name` (find it in your azd command output or `/.azure/*/.env` file). You'll also be prompted to authenticate with Microsoft - click **Allow** and login with your Azure subscription email.

> [!TIP]
> Successful connect shows the number of tools the server has. You can see more details on the interactions between VS Code and server by clicking on **More... -> Show Output** above the server name.

## Redeploy and clean up

**Redeploy all:** Run `azd up` as many times as needed to deploy code updates.

**Redeploy one service:** Use `azd deploy api` or `azd deploy weather` to redeploy a specific app.

**Clean up:** Delete all Azure resources when done:

```
azd down
```

## Next Steps

+ Add [API Management](https://github.com/Azure-Samples/remote-mcp-apim-functions-python) to your MCP server
+ Enable VNET using `VNET_ENABLED=true` flag
+ Learn more about [related MCP efforts from Microsoft](https://github.com/microsoft/mcp/tree/main/Resources)

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Connection refused | Ensure Azurite is running (`docker run -p 10000:10000 -p 10001:10001 -p 10002:10002 mcr.microsoft.com/azure-storage/azurite`) |
| API version not supported by Azurite | Pull the latest Azurite image (`docker pull mcr.microsoft.com/azure-storage/azurite`) then restart Azurite and the app |
| `mvn clean package` fails | Ensure JDK 17+ is on your PATH (`java -version`) |
