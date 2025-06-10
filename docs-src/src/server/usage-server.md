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
    <version>1.2</version>
</dependency>
```

We can find the latest version:
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-server?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)

The server module includes:

- `McpServer` core processing engine
- `FeatureMgr` for annotation-based feature discovery
- `@Tool`, `@Resource`, `@Prompt` annotations
- Transport implementations for HTTP/SSE and Stdio

## Creating a Server

To create an MCP server, you need to:

1. Define Service Classes: Create classes annotated with `@McpService`
1. Annotate Methods: Use `@Tool`, `@Prompt`, or `@Resource` annotations
1. Initialize Feature Manager: Scan packages for annotations
1. Configure Transport: Set up HTTP/SSE or Stdio transport, and some details of server
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

## Server  Feature Management

The Feature Management system operates through a centralized `FeatureMgr` class that coordinates package scanning, annotation processing, and feature
storage.The system uses reflection to discover annotated methods and stores feature metadata in concurrent hash maps for thread-safe runtime access.

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

Server configuration includes annotation-driven feature discovery through `FeatureMgr.init()` with package scanning `FeatureMgr`. This automatically
discovers and registers `@McpService` annotated classes containing `@Tool`, `@Resource`, and `@Prompt` methods.

### Initialize Feature Manager

The `FeatureMgr.init()` method orchestrates the entire annotation discovery process. It begins by scanning specified packages for classes annotated
with `@McpService`.

```java
FeatureMgr mgr=new FeatureMgr();
        mgr.init("com.foo.myproduct");.
```

## Server Configuration

After feature manager initialization with package scanning, we can configure the server with:

- Server instance creation with transport layer setup
- Server configuration with name and version
- Connection timeout settings
- The page size for paginated responses

Server configuration is handled through the ServerConfig class, which contains essential server metadata `McpServerInitialize`. The configuration
includes server name, version, and supported protocol versions `McpServerInitialize`.

During initialization, the server processes protocol version negotiation where it responds with the highest supported version or matches the client's
requested version if supported `McpServerInitialize`.

```java
FeatureMgr mgr=new FeatureMgr();
        mgr.init("com.foo.myproduct");

        McpServer server = new McpServer();
        server.setTransport(new ServerStdio(server));

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("MY_MCP_Server");
        serverConfig.setVersion("1.0");
        serverConfig.setPageSize(8);
        server.setServerConfig(serverConfig);
```