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

### Client roots change callback

Register a callback to receive `notifications/roots/list_changed`:

```java
server.setRootsChangedHandler(sessionId -> {
    // Invalidate application-owned cached roots for this session.
    rootsCache.remove(sessionId);
});
```

`rootsCache` is your application's thread-safe cache, keyed by session ID. This callback works with STDIO, legacy SSE and Streamable HTTP for all three supported protocol versions. It runs only for clients that advertised `capabilities.roots.listChanged: true`. The notification contains no updated roots; query them separately with `server.listRoots(sessionId, timeout)` if needed.

The handler runs on the receiving thread and must return promptly. Do not call blocking `listRoots` directly inside it: schedule refreshes on an application-owned executor, since the receiving thread may be needed to process the response. Handlers must be thread-safe; asynchronous refreshes must handle session closure and timeouts. The application owns executor shutdown. Callback runtime exceptions are logged without producing a JSON-RPC response or terminating reception. Pass `null` to `setRootsChangedHandler` to unregister. The SDK creates no executor and does not automatically refresh roots.

### Legacy SSE Origin validation

Before opening an SSE stream or dispatching a POST message, HTTP adapters must call `serverSse.isOriginAllowed(request.getHeader("Origin"))` and return HTTP 403 when it is false. Both bundled Spring and Tomcat adapters perform this check. Native clients without an Origin header remain supported; supplied origins require an exact match in `ServerConfig.allowedOrigins`. Empty and opaque `null` origins are rejected. Wildcards are not supported.

```java
config.setAllowedOrigins(java.util.Collections.singletonList("https://app.example.com"));
```

Origin validation is separate from authentication and does not automatically configure CORS response headers.

## MCP Server SDK Setup

Add this dependency to build MCP servers:

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.6</version>
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

AJ-MCP automatically discovers, registers, and manages MCP features (tools, resources, and prompts) through
annotation-based scanning.
This system enables developers to expose functionality simply by annotating methods with `@Tool`, `@Resource`, or
`@Prompt` annotations within classes
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

Each server uses a `FeatureMgr` instance to coordinate package scanning, annotation processing, and feature storage. The
stores are instance-scoped, so multiple servers in the same JVM do not share tools, resources, or prompts.

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

Calling `FeatureMgr.init()` scans a package and registers methods from classes annotated with `@McpService`. A class
that cannot be loaded is logged and skipped without aborting the rest of the scan.

### Initialize Feature Manager

The `FeatureMgr.init()` method orchestrates the entire annotation discovery process. It begins by scanning specified
packages for classes annotated
with `@McpService`.

```java
FeatureMgr mgr = new FeatureMgr();
mgr.

init("com.foo.myproduct");
```

## Server Configuration

After feature manager initialization with package scanning, we can configure the server with:

- Server instance creation with transport layer setup
- Server configuration with name and version
- The page size for paginated responses

`ServerConfig` contains the server name, version, supported protocol versions, and page size.

During initialization, the server returns the requested protocol version when it is supported; otherwise, it returns its
highest supported version.

```java
FeatureMgr mgr = new FeatureMgr();
mgr.

init("com.foo.myproduct");

McpServer server = new McpServer();
server.

setFeatureMgr(mgr);
server.

setTransport(new ServerStdio(server));

ServerConfig serverConfig = new ServerConfig();
serverConfig.

setName("MY_MCP_Server");
serverConfig.

setVersion("1.0");
serverConfig.

setPageSize(8);
server.

setServerConfig(serverConfig);

server.

start();
```

## Transport and lifecycle rules

Set `ServerConfig.title` for an optional server display label; initialization emits it only for 2025-06-18.
Clients can set `McpClient.builder().clientTitle(...)` (included when requesting 2025-06-18).
`StructuredToolResult.setMeta(...)` carries application metadata into the wire tool result without flattening it.
Streamable HTTP DELETE validates the version header before removing a session; an explicitly unsupported header
is also rejected on initialization. Legacy HTTP/SSE sends the negotiated header on POSTs, including initialized.

For a runnable Spring Boot example, see `samples/server/spring-streamable-http` in the repository.
Run its `DemoApplication`, then `com.foo.StreamableHttpClientExample` from `samples/client`.
The default endpoint is `http://127.0.0.1:8081/mcp`. This Java 8 sample demonstrates asynchronous GET,
POST/DELETE, progress and reverse roots requests, with bilingual READMEs and random-port integration tests.

`@Tool(title = "Weather")` exposes `annotations.title` in MCP 2025-03-26 and later; 2025-06-18 also
exposes the top-level tool `title`. Both are omitted for 2024-11-05.
Use `server.sendProgress(sessionId, token, progress, total, "Processing...")` or the current-session
overload `server.sendProgress(token, progress, total, "Processing...")` to send a progress message.
The optional `message` is omitted for 2024-11-05; existing overloads remain unchanged.

- `ServerStdio` exchanges one JSON-RPC message per line. Keep `System.out` reserved for protocol output.
- `ServerSse` is the legacy two-endpoint adapter: open a session with `openSession(...)`, route POST messages to
  `handle(sessionId, body)`, remove the connection on disconnect, and close the adapter on shutdown.
- `ServerStreamableHttp` uses one endpoint: delegate `POST` to `post(body, headers)`, optional `GET` event streams to
  `openEventStream(sessionId, writer, headers)`, and `DELETE` to `delete(sessionId, headers)`. Copy the returned
  `HttpResult` status, headers, content type, and body to the framework response.

Initialization negotiates a supported version and creates the Streamable HTTP session. With MCP `2025-06-18`, subsequent
Streamable HTTP requests must include the negotiated `MCP-Protocol-Version` header. `strictLifecycle` is enabled by
default, so normal requests require `initialize` followed by `notifications/initialized`. JSON-RPC batch messages are
intentionally unsupported.

Keep GET responses open asynchronously and flush headers before returning from the controller. Call
`closeEventStream(sessionId, writer)` on framework completion/error/timeout; the adapter closes its registered writer.
GET heartbeats run every 15 seconds. `ServerConfig.sessionIdleTimeout` defaults to 30 minutes and includes sessions
without GET; active POSTs are protected, and heartbeat traffic alone does not renew idle sessions. Close removes all
sessions. POST replies remain JSON; client responses/notifications are accepted with an empty 202 response.

`clientRequestTimeout` supplies a finite 60-second default for reverse calls with null/zero timeout. Tool integral
arguments reject fractions/overflow as `INVALID_PARAMS`. Logging thresholds apply separately to each session.
Completion methods may accept `(String value, Map<String, String> arguments)` to read the immutable
2025-06-18 `context.arguments`; the existing single-String signature is supported.

STDIO uses UTF-8. Configure `stdioWorkers` (default 16) and `stdioQueueCapacity` (default 256) before constructing
`ServerStdio`. Full queues produce a busy error while handshake, ping, cancellation and reverse replies remain usable.
