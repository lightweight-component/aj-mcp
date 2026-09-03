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
    <version>1.5</version>
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

First, create the transport that matches the MCP server.

### Stdio Transport

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
McpTransport transport = StreamableHttpTransport.builder()
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

> Current limitations: request-scoped streaming over a POST `text/event-stream` response is buffered rather than
> processed incrementally; use ordinary JSON POST responses. The optional GET event stream has no reconnect/resumption
> policy, and initialization does not wait for that stream to become ready. Do not rely on incremental POST progress or
> server requests delivered through a POST response until this limitation is removed.

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
| requestTimeout  | Timeout applied to every request, including initialization and health checks. The default is 60 seconds; zero means wait indefinitely, and negative values are rejected.          | Duration      | `Duration.ofSeconds(60)` |

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
