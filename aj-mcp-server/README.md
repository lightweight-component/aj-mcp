[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-server?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-server)
[![Javadoc](https://img.shields.io/badge/javadoc-1.5-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp-server)
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Email](https://img.shields.io/badge/Contact--me-Email-orange.svg)](mailto:frank@ajaxjs.com)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![中文](https://img.shields.io/badge/lang-中文-red)](./README.zh-CN.md)

# Lightweight Java MCP Server

AJ-MCP Server is a lightweight MCP server SDK for Java. It turns ordinary Java methods into MCP tools, resources,
resource templates, prompts, and completion providers through annotations, then serves them over STDIO, legacy HTTP/SSE,
or Streamable HTTP.

## Features

- Java 8 and Spring Boot 2.x compatible.
- Package scanning for classes annotated with `@McpService`.
- Annotation-based tools, resources, resource templates, prompts, and argument completion.
- Automatic JSON Schema generation for tool inputs, including non-null schemas for parameterless tools.
- MCP revisions `2024-11-05`, `2025-03-26`, and `2025-06-18`, selected during initialization.
- Pagination, resource subscriptions, cancellation, progress/logging notifications, roots, sampling, and elicitation.
- Session-aware request routing and lifecycle management for STDIO and HTTP transports.
- Structured JSON-RPC errors for invalid methods, parameters, and feature invocations.

## Source code

[Github](https://github.com/lightweight-component/aj-mcp) | [Gitcode](https://gitcode.com/lightweight-component/aj-mcp)

## Links

[Tutorials](https://mcp.ajaxjs.com/) | [Java Documents](https://javadoc.io/doc/com.ajaxjs/aj-mcp-server)

## Install

Runs on Java8+. Maven:

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.5</version>
</dependency>
```

`aj-mcp-common` is included transitively.

## Quick start: define MCP features

Place MCP methods in a public no-argument class annotated with `@McpService`:

```java

@McpService
public class DemoFeatures {
    @Tool(description = "Adds two integers")
    public String add(@ToolArg("a") Integer a,
                      @ToolArg("b") Integer b) {
        return String.valueOf(a + b);
    }

    @Tool(description = "Returns the server time")
    public String serverTime() {
        return new Date().toString();
    }

    @Resource(uri = "config:///application", mimeType = "text/plain",
            description = "Current application configuration")
    public ResourceContentText configuration() {
        ResourceContentText content = new ResourceContentText();
        content.setUri("config:///application");
        content.setMimeType("text/plain");
        content.setText("mode=production");
        return content;
    }

    @Prompt(description = "Creates a code-review prompt")
    public PromptMessage review(@PromptArg("language") String language) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Review this " + language + " code"));
        return message;
    }
}
```

Every Java parameter of a tool must have `@ToolArg`. Arguments are required by default; set `required = false` when
omission is valid. A parameterless tool is valid and is advertised with an empty object input schema.

## Quick start: STDIO server

```java
public static void main(String[] args) {
    FeatureMgr features = new FeatureMgr();
    features.init("com.example.mcp");

    ServerConfig config = new ServerConfig();
    config.setName("example-server");
    config.setVersion("1.0.0");

    McpServer server = new McpServer();
    server.setFeatureMgr(features);
    server.setServerConfig(config);
    server.setTransport(new ServerStdio(server));
    server.start();
}
```

STDIO uses one JSON-RPC message per line. Do not write application output to `System.out`, because it corrupts the
protocol stream. Configure logging to standard error or a file. `server.start()` blocks until standard input closes or
the transport is stopped.

## Legacy HTTP/SSE

`ServerSse` is a framework-neutral adapter for the original MCP HTTP/SSE transport. Your Servlet or controller must:

1. expose a `GET` endpoint with `Content-Type: text/event-stream`;
2. generate a client/session ID and call `openSession(id, writer, messageEndpoint)`;
3. expose the advertised `POST` message endpoint and pass its JSON body to `handle(id, body)`;
4. call `removeConnection(id)` when the request disconnects;
5. close the transport when the application stops.

```java
McpServer server = new McpServer();
server.

setFeatureMgr(features);
server.

setServerConfig(config);

ServerSse transport = new ServerSse(server);
server.

setTransport(transport);
server.

start();
```

See the [Spring Boot](../samples/server/spring) and [embedded Tomcat](../samples/server/tomcat) samples for controller
and Servlet wiring.

## Streamable HTTP

`ServerStreamableHttp` is also Servlet-framework-neutral. Mount one endpoint and delegate:

- `POST` requests to `post(body, headers)`;
- optional `GET` event streams to `openEventStream(sessionId, writer, headers)`;
- `DELETE` requests to `delete(sessionId, headers)`.

Copy the returned status, headers, content type, and body from `HttpResult` to the framework response. Initialization
creates the session and returns `Mcp-Session-Id`. For `2025-06-18`, subsequent requests must include the negotiated
`MCP-Protocol-Version` header. Configure browser origins with `ServerConfig.allowedOrigins`; an empty list rejects
requests that supply an `Origin` header.

JSON-RPC batch requests are intentionally unsupported.

Current Streamable HTTP limitations: ordinary JSON POST responses and the optional GET event stream are supported, but
the server does not yet produce request-scoped POST SSE responses. A session that never opens the GET stream should be
terminated through the `DELETE` endpoint; automatic idle-session expiry is not implemented.

## Configuration

`ServerConfig` controls:

| Property           | Meaning                                                 | Default                   |
|--------------------|---------------------------------------------------------|---------------------------|
| `name`, `version`  | Server identity returned during initialization          | unset                     |
| `pageSize`         | Number of tools/resources/prompts per page              | `3`                       |
| `protocolVersions` | Supported revisions, newest first                       | all implemented revisions |
| `strictLifecycle`  | Require initialize → initialized before normal requests | `true`                    |
| `allowedOrigins`   | Accepted browser Origin values for Streamable HTTP      | empty                     |

Create one `FeatureMgr` per server and assign it with `setFeatureMgr`. Feature stores are instance-scoped, so scanning
one server does not populate another server.

## Shutdown

Close the configured transport when the application is stopping. This removes sessions, stops heartbeat/executor
threads, interrupts STDIO processing where applicable, and releases writers. Framework applications should call it from
their normal destruction hook (`@PreDestroy`, servlet destruction, or equivalent).
