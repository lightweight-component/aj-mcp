---
title: MCP Client SDK Setup
subTitle: 2024-12-05 by Frank Cheung
description: MCP Client SDK Setup
date: 2022-01-05
tags:
  - client setup
layout: layouts/docs.njk
---

# MCP Client SDK Setup

## Install the Dependency

Add the AJ MCP client dependency:

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-client</artifactId>
    <version>1.6</version>
</dependency>
```

We can find the latest version:
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-client?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)

## Concepts

The client SDK implementation consists of two main components:

- Transport: Manages the subprocess and handles low-level message exchange.
- MCP Client: Provides a high-level API for using the transport, implementing the MCP protocol.

To use the client, first create an appropriate transport and then build an `McpClient` with that transport.
The client supports three transport mechanisms: standard I/O (STDIO), the legacy two-endpoint HTTP/SSE transport, and
Streamable HTTP.

## Setup the Transport

For an end-to-end Streamable HTTP walkthrough, run the repository's
`samples/server/spring-streamable-http` server and `samples/client` entry
`com.foo.StreamableHttpClientExample`. It waits for GET readiness, calls tools, receives progress,
answers roots requests, reads resources, retrieves prompts and closes the session.

First, create the transport that matches the MCP server.

### Automatic HTTP transport discovery

When the server's HTTP transport is unknown, use `AutoHttpTransport`:

```java
McpTransport transport = new AutoHttpTransport("http://localhost:8080/mcp");
McpClient client = McpClient.builder().transport(transport).build();
client.initialize();
// Use the client, then call client.close().
```

The initial POST tries Streamable HTTP. HTTP 400/404/405/415 triggers a GET to the **same URL**;
an SSE `endpoint` event supplies the legacy POST address. No `/sse` path is guessed. Authentication errors,
rate limits, server errors, network timeouts, and JSON-RPC errors do not trigger fallback. Once initialized,
business calls never switch transports; a Streamable HTTP session 404 follows session recovery instead.
Version negotiation is independent: the transport does not force 2024-11-05, and respects the client's supported versions.

The builder accepts `endpointUrl`, `timeout`, `requestHeaders`, and `openEventStream` (for optional modern GET only;
legacy SSE always needs GET). Headers are retained on fallback. Legacy endpoint events must resolve to the same
origin to avoid forwarding credentials elsewhere. `isLegacySse()` reports the selected legacy implementation.
Explicit `StreamableHttpTransport` and `HttpMcpTransport` remain available and never auto-switch.

### Stdio Transport Examples

Stdio stands for standard input/output. In this transport, the client launches a local MCP server subprocess and
exchanges one JSON-RPC message per line through its standard streams.

``` java
// The MCP server is a Java program that communicates over stdio.
McpTransport transport = StdioTransport.builder()
    .command(Arrays.asList("java", "-jar", "C:\\app\\my-app-jar-with-dependencies.jar"))
    .logEvents(true)
    .build();
```

Here is an example using a `.exe` program:

``` java
// The MCP server is a native executable that communicates over stdio.
McpTransport transport = StdioTransport.builder()
    .command(Arrays.asList("C:\\app\\my-app.exe", "-token", "dd4df2sx32ds"))
    .logEvents(true)
    .build();
```

Set `logEvents` to `true` to log outgoing protocol messages while debugging. The transport also consumes stderr so that
a child process cannot block on a full error pipe.

### Legacy HTTP/SSE Transport

The legacy transport uses an SSE endpoint for server-to-client messages and a server-advertised POST endpoint for client
requests. It is useful when connecting to older MCP servers.

``` java
McpTransport transport = HttpMcpTransport.builder()
    .sseUrl("http://localhost:8080/sse")
    .logRequests(true)
    .logResponses(true)
    .build();
```

The `sseUrl` is required. It specifies the URL of the SSE endpoint where the MCP server is listening for incoming
connections.

### Streamable HTTP (2025-03-26 / 2025-06-18)

Newer revisions use one HTTP endpoint:

```java
StreamableHttpTransport transport = StreamableHttpTransport.builder()
        .endpointUrl("http://localhost:8080/mcp")
        .openEventStream(true)
        .build();

McpClient client = McpClient.builder()
        .transport(transport)
        .protocolVersion("2025-06-18")
        .build();
client.

initialize();
```

For OAuth, pass an `Authorization` Bearer token through `requestHeaders`. The SDK retains the returned session ID and
automatically sends the negotiated `MCP-Protocol-Version` on subsequent requests.

Set `openEventStream(true)` when the client advertises Roots, Sampling, or Elicitation handlers. Those server-initiated
requests are received through the optional GET event stream, which opens asynchronously after initialization.

POST SSE responses are consumed incrementally, including progress and reverse requests before the final response.
GET failure does not fail independent POST calls. For GET-dependent workflows, await
`transport.getEventStreamReady().get(5, TimeUnit.SECONDS)` and inspect `isEventStreamOpen()` /
`getEventStreamFailure()`. GET disconnects retry at most five times with exponential backoff (200 ms to 5 seconds),
using `Last-Event-ID` when available; replay requires server support. HTTP 4xx stops retries. Closing sends a
best-effort DELETE with a two-second deadline. STDIO always encodes messages as UTF-8.

## McpClient

The MCP Client serves as a bridge between local applications and remote tool implementations.

``` java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(transport)
        .build();
```

Usually, you should set the `clientName` and `clientVersion` properties.
The `clientName` property is used to identify the client to the MCP server, while the  `clientVersion` property is used
to indicate the version of the
client.

All properties are listing below:

| Property        | Note                                                                                                                                                                              | Type of value | Example of value         |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|--------------------------|
| clientName      | Sets the name that the client will use to identify itself to the MCP server in the initialization message.                                                                        | String        | myapp/foo-app            |
| clientVersion   | Sets the version string that the client will use to identify itself to the MCP server in the initialization message. The default value is "1.0".                                  | String        | 1.0/2.1.2                |
| protocolVersion | Sets the protocol version that the client will advertise in the initialization message. The default value right now is "2024-11-05", but will change over time in later versions. | String        | 2024-11-05               |
| requestTimeout  | Timeout for requests, initialization and health checks. Null or zero uses the finite 60-second default; negative values are rejected. | Duration | `Duration.ofSeconds(60)` |

Please note that after creating the McpClient, you should call `mcpClient.initialize();` right away.
The next section describes protocol initialization.

``` java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();
        
mcpClient.initialize();
```

Close the client when it is no longer needed. Closing the transport releases HTTP/SSE requests or the stdio subprocess
and completes outstanding requests exceptionally.

``` java
try (IMcpClient mcpClient2 = McpClient.builder().transport(transport).build()) {
    mcpClient2.initialize();
    ...
} catch (Exception e) {
   throw new RuntimeException(e);
}
```

The MCP Client follows a layered architecture with a clean separation between the interface definition and its
implementation. The client relies on
the transport layer for actual communication with the server, abstracting the communication details to support different
transport mechanisms.


<style>
table th:nth-child(2) {
 min-width: 400px;
}
table th:nth-child(3), table td:nth-child(3) {
 min-width: 120px!important;
 width: 120px;
}

table td:nth-child(2) {
 text-align: left;
}
</style>
