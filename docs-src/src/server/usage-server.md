---
title: MCP Server SDK Usage
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK Usage
date: 2022-01-05
tags:
  - MCP Server SDK Usage
layout: layouts/docs.njk
---

# MCP Server SDK Usage

## MCP Server SDK Setup

Add this dependency to build MCP servers:

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.4</version>
</dependency>
```

We can find the latest version:
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-server?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-server)

The server module includes:

- `McpServer` core processing engine
- `FeatureMgr` for annotation-based feature discovery
- `@Tool`, `@Resource`, `@Prompt` annotations
- Transport implementations for STDIO, legacy HTTP/SSE, and Streamable HTTP

## Creating a Server

To create an MCP server, you need to:

1. Define Service Classes: Create classes annotated with `@McpService`
1. Annotate Methods: Use `@Tool`, `@Prompt`, or `@Resource` annotations
1. Initialize Feature Manager: Scan packages for annotations
1. Configure Transport: Set up STDIO, legacy HTTP/SSE, or Streamable HTTP and its server details
1. Start Server: Call `server.start()`

## Creating MCP Service Class

AJ-MCP automatically discovers, registers, and manages MCP features (tools, resources, and prompts) through annotation-based scanning.
This system enables developers to expose functionality simply by annotating methods with `@Tool`, `@Resource`, or `@Prompt` annotations within classes
marked with `@McpService`.

```java

@McpService
public class MyServerFeatures {
    @Tool(description = "Echoes a string")
    public String echoString(@ToolArg(description = "Input string") String input) {
        return input;
    }

    @Prompt(description = "Basic greeting prompt")
    public PromptMessage greeting(@PromptArg(description = "Name") String name) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello " + name));
        return message;
    }
}
```

## Server Feature Management

Each server uses a `FeatureMgr` instance to coordinate package scanning, annotation processing, and feature storage. The stores are instance-scoped, so multiple servers in the same JVM do not share tools, resources, or prompts.

### Annotation System

The annotation system is built around several key annotations that mark classes and methods for MCP exposure:

| Annotation  | Target    | Purpose                             |
|-------------|-----------|-------------------------------------|
| @McpService | Class     | Marks a class for service discovery |
| @Tool       | Method    | Exposes a method as an MCP tool     |
| @ToolArg    | Parameter | Defines tool method parameters      |
| @Resource   | Method    | Exposes a method as an MCP resource |
| @Prompt     | Method    | Exposes a method as an MCP prompt   |
| @PromptArg  | Parameter | Defines prompt method parameters    |

Calling `FeatureMgr.init()` scans a package and registers methods from classes annotated with `@McpService`. A class that cannot be loaded is logged and skipped without aborting the rest of the scan.

### Initialize Feature Manager

The `FeatureMgr.init()` method orchestrates the entire annotation discovery process. It begins by scanning specified packages for classes annotated
with `@McpService`.

```java
FeatureMgr mgr = new FeatureMgr();
mgr.init("com.foo.myproduct");
```

## Server Configuration

After feature manager initialization with package scanning, we can configure the server with:

- Server instance creation with transport layer setup
- Server configuration with name and version
- The page size for paginated responses

`ServerConfig` contains the server name, version, supported protocol versions, and page size.

During initialization, the server returns the requested protocol version when it is supported; otherwise, it returns its highest supported version.

```java
FeatureMgr mgr = new FeatureMgr();
mgr.init("com.foo.myproduct");

McpServer server = new McpServer();
server.setFeatureMgr(mgr);
server.setTransport(new ServerStdio(server));

ServerConfig serverConfig = new ServerConfig();
serverConfig.setName("MY_MCP_Server");
serverConfig.setVersion("1.0");
serverConfig.setPageSize(8);
server.setServerConfig(serverConfig);

server.start();
```

## Transport and lifecycle rules

- `ServerStdio` exchanges one JSON-RPC message per line. Keep `System.out` reserved for protocol output.
- `ServerSse` is the legacy two-endpoint adapter: open a session with `openSession(...)`, route POST messages to `handle(sessionId, body)`, remove the connection on disconnect, and close the adapter on shutdown.
- `ServerStreamableHttp` uses one endpoint: delegate `POST` to `post(body, headers)`, optional `GET` event streams to `openEventStream(sessionId, writer, headers)`, and `DELETE` to `delete(sessionId, headers)`. Copy the returned `HttpResult` status, headers, content type, and body to the framework response.

Initialization negotiates a supported version and creates the Streamable HTTP session. With MCP `2025-06-18`, subsequent Streamable HTTP requests must include the negotiated `MCP-Protocol-Version` header. `strictLifecycle` is enabled by default, so normal requests require `initialize` followed by `notifications/initialized`. JSON-RPC batch messages are intentionally unsupported.
