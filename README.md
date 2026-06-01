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

# Remote MCP Servers using Azure Functions (Java)

A collection of samples for building and deploying remote MCP servers to Azure using Azure Functions (Java). Each sample runs locally with debugging and deploys to Azure with `azd up`.

All samples are configured with [built-in authentication](https://learn.microsoft.com/en-us/azure/app-service/overview-authentication-authorization) using Microsoft Entra as the identity provider. You can also use [API Management](https://learn.microsoft.com/azure/api-management/secure-mcp-servers) to secure the server, or add network isolation with VNET.

If you're looking for samples in other languages, see: [.NET/C#](https://github.com/Azure-Samples/remote-mcp-functions-dotnet) | [Node.js/TypeScript](https://github.com/Azure-Samples/remote-mcp-functions-typescript) | [Python](https://github.com/Azure-Samples/remote-mcp-functions-python)

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/Azure-Samples/remote-mcp-functions-java)

## Prerequisites

+ [JDK 17](https://learn.microsoft.com/java/openjdk/download) (or newer)
+ [Apache Maven](https://maven.apache.org/download.cgi)
+ [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local) >= `4.0.7030`
+ [Azure Developer CLI](https://aka.ms/azd) (for deployment)
+ [Visual Studio Code](https://code.visualstudio.com/) + [Azure Functions extension](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-azurefunctions) (recommended)

## Samples in this repo

| Project | Path | Description |
|---------|------|-------------|
| **FunctionsMcpTool** | [`samples/FunctionsMcpTool/`](samples/FunctionsMcpTool/) | MCP tools, prompts, and structured content; includes blob storage tools, prompt templates, image content, and multi-content responses. |
| **McpWeatherApp** | [`samples/McpWeatherApp/`](samples/McpWeatherApp/) | MCP App with an interactive weather UI (MCP tool + resource + metadata). |

## Run locally

Each project has its own README with instructions for running locally, connecting to the MCP server, deploying to Azure, and more:

+ [FunctionsMcpTool README](samples/FunctionsMcpTool/README.md)
+ [McpWeatherApp README](samples/McpWeatherApp/README.md)

## Next Steps

+ Learn more about the [Azure Functions MCP extension](https://learn.microsoft.com/azure/azure-functions/functions-bindings-mcp?pivots=programming-language-typescript)
+ Follow our blog posts on [Azure SDK Blog](https://devblogs.microsoft.com/azure-sdk) and [Tech Community](https://techcommunity.microsoft.com/category/azure/blog/appsonazureblog) for updates.