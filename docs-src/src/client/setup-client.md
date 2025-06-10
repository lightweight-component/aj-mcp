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

## Install Dependency

We’ll need the AJ MCP SDK for making API requests. Install them with:

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-client</artifactId>
    <version>1.3</version>
</dependency>
```

We can find the latest version:
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-client?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)

## Concepts

The client SDK implementation consists of two main components:

- Transport: Manages the subprocess and handles low-level message exchange.
- MCP Client: Provides a high-level API for using the transport, implementing the MCP protocol.

To start using the MCP client, you must first these two main components:
first to create a appropriate transport instance, and then to create a client instance with that transport.
The client supports different transport mechanisms, primarily Server-Sent Events (SSE) and standard I/O (stdio).

## Setup the Transport

First, we need to create the transport. There are two types of transport you can choose from for your application, depending on the type of your MCP
server.

### Stdio Transport

'Stdio' stands for Standard Input/Output, by using command line to interact between the programme and human in short. But here is between the MCP
Client
and the MCP Server. Usually, we use stdio for the local application, such as a `*.exe` programme or a Java Jar programme, and so on.

``` java
// The MCP server is a Java programme, runs on stdio.
McpTransport transport = StdioTransport.builder()
    .command(Arrays.asList("java", "-jar", "C:\\app\\my-app-jar-with-dependencies.jar"))
    .logEvents(true)
    .build();
```

Let's take a look at a `.exe` programme as an example:

``` java
// The MCP server is an executable programme, runs on stdio.
McpTransport transport = StdioTransport.builder()
    .command("C:\\app\\my-app.exe", "-token", "dd4df2sx32ds"))
    .logEvents(true)
    .build();
```

If you want to check out the fully logs of the client, you can set `logEvents` to `true`. This approach is beneficial for debugging purposes or for
gaining a deeper understanding of the MCP protocol's JSON-based messaging format.

### SSE Transport

The SSE Transport enables bidirectional communication between MCP clients and servers using the HTTP protocol with Server-Sent Events. This transport
method is particularly useful for web-based applications

``` java
McpTransport transport = HttpMcpTransport.builder()
    .sseUrl("http://localhost:8080/sse")
    .logRequests(true)
    .logResponses(true)
    .build();
```

The `sseUrl` is required. It specifies the URL of the SSE endpoint where the MCP server is listening for incoming connections.

## McpClient

The MCP Client serves as a bridge between local applications and remote tool implementations.

``` java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(transport)
        .build();
```

Usually we fill the `clientName` and `clientVersion` properties.
The `clientName` property is used to identify the client to the MCP server, while the  `clientVersion` property is used to indicate the version of the
client.

All properties are listing below:

| Property        | Note                                                                                                                                                                              | Type of value | Example of value         |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|--------------------------|
| clientName      | Sets the name that the client will use to identify itself to the MCP server in the initialization message.                                                                        | String        | myapp/foo-app            |
| clientVersion   | Sets the version string that the client will use to identify itself to the MCP server in the initialization message. The default value is "1.0".                                  | String        | 1.0/2.1.2                |
| protocolVersion | Sets the protocol version that the client will advertise in the initialization message. The default value right now is "2024-11-05", but will change over time in later versions. | String        | 2024-11-05               |
| requestTimeout  | Sets the timeout for tool execution. This value applies to each tool execution individually. The default value is 60 seconds. A value of zero means no timeout.                   | Duration      | `Duration.ofSeconds(60)` |

Please note that after creating the McpClient, you should call `mcpClient.initialize();` right away.
We'll talk about the initialization of MCP in next section.

``` java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();
        
mcpClient.initialize();
```

Finally, please remember to close the McpClient resources by calling the `close()` method, or using autocloseable.

``` java
try(IMcpClient mcpClient2 = McpClient.builder().transport(transport).build()){
    ...
} catch (Exception e) {
   throw new RuntimeException(e);
}
```

The MCP Client follows a layered architecture with a clean separation between the interface definition and its implementation. The client relies on
the transport layer for actual communication with the server, abstracting the communication details to support different transport mechanisms.


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
