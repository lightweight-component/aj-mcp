[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-client?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)
[![Javadoc](https://img.shields.io/badge/javadoc-1.5-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp-client)
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Email](https://img.shields.io/badge/Contact--me-Email-orange.svg)](mailto:frank@ajaxjs.com)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![中文](https://img.shields.io/badge/lang-中文-red)](./README.zh-CN.md)

# Lightweight Java MCP Client

AJ-MCP Client is a lightweight MCP client for Java. It provides a synchronous, Java-8-friendly API for discovering and calling tools, reading resources, rendering prompts, and using the other capabilities exposed by an MCP server.

## Features

- STDIO client transport for starting and communicating with a local child process.
- Legacy HTTP/SSE transport for MCP servers that expose separate SSE and POST endpoints.
- Streamable HTTP transport for MCP `2025-03-26` and `2025-06-18` servers.
- Negotiation of protocol revisions `2024-11-05`, `2025-03-26`, and `2025-06-18`.
- Tools, resources, resource templates, prompts, completion, pagination, subscriptions, notifications, cancellation, and health checks.
- Client handlers for roots, sampling, and `2025-06-18` elicitation.
- Configurable request timeout and cleanup of pending requests when a transport closes.

## Source code

[Github](https://github.com/lightweight-component/aj-mcp) | [Gitcode](https://gitcode.com/lightweight-component/aj-mcp)

## Link

[User Manual](https://mcp.ajaxjs.com/)  | [Java Documents](https://javadoc.io/doc/com.ajaxjs/aj-mcp-client)

## Install

Runs on Java8+. Maven:

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-client</artifactId>
    <version>1.5</version>
</dependency>
```

`aj-mcp-common` is included transitively.

## Quick start: STDIO

The convenience factory starts the command as a child process and completes MCP initialization before returning:

```java
try (McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/path/to/server.jar")) {
    for (ToolItem tool : client.listTools())
        System.out.println(tool.getName() + " - " + tool.getDescription());

    String result = client.callTool(
            "echoString", "{\"input\":\"Hello from Java\"}");
    System.out.println(result);
}
```

For a custom configuration, build the transport and client separately:

```java
McpTransport transport = StdioTransport.builder()
        .command(Arrays.asList("java", "-jar", "/path/to/server.jar"))
        .logEvents(true)
        .build();

try (McpClient client = McpClient.builder()
        .transport(transport)
        .requestTimeout(Duration.ofSeconds(30))
        .build()) {
    client.initialize();
    client.checkHealth();
}
```

The child server must reserve standard output for newline-delimited JSON-RPC messages. Its logs should go to standard error.

## Quick start: Streamable HTTP

```java
McpTransport transport = StreamableHttpTransport.builder()
        .endpointUrl("http://localhost:8080/mcp")
        .openEventStream(true)
        .timeout(Duration.ofSeconds(30))
        .build();

try (McpClient client = McpClient.builder()
        .transport(transport)
        .protocolVersion(ProtocolVersion.V_2025_06_18.value())
        .requestTimeout(Duration.ofSeconds(30))
        .build()) {
    client.initialize();
    System.out.println("Negotiated: " + client.getNegotiatedProtocolVersion());
    client.listResources().forEach(resource -> System.out.println(resource.getUri()));
}
```

`openEventStream` enables the optional long-lived GET channel for server-originated messages. The transport retains the session ID returned by initialization and sends the negotiated protocol header when required.

For an older server with separate SSE and message endpoints, use `HttpMcpTransport`:

```java
McpTransport transport = HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .logRequests(false)
        .logResponses(false)
        .build();
```

## Common operations

```java
List<ToolItem> tools = client.listTools();
McpPage<ToolItem> page = client.listToolPage(null);
CallToolResult.CallToolResultDetail detail =
        client.callToolResult(new CallToolRequest("weather", "{\"city\":\"Guangzhou\"}"));

List<ResourceItem> resources = client.listResources();
GetResourceResult.ResourceResultDetail resource = client.readResource("file:///readme");

List<PromptItem> prompts = client.listPrompts();
GetPromptResult.PromptResultDetail prompt =
        client.getPrompt("review", Collections.<String, Object>singletonMap("language", "Java"));
```

Cursor methods such as `listToolPage`, `listResourcePage`, and `listPromptPage` are preferred when a server returns opaque cursors. The older integer page methods remain available for compatibility.

## Lifecycle and errors

- Call `initialize()` exactly once before normal requests when constructing a client manually.
- Set `requestTimeout` to a positive duration for bounded behavior. A zero duration explicitly disables the client-side timeout.
- Use try-with-resources or call `close()` in `finally`. Closing fails pending requests and releases the transport, worker threads, HTTP connections, and child process.
- `callToolResult()` preserves the full MCP result, including structured content and `isError`. `callTool()` is a text convenience method.
- Transport and protocol failures are surfaced as runtime exceptions; tool-level failures may be represented by MCP tool results.

## Protocol support

The client advertises a preferred `protocolVersion` and a list of `supportedProtocolVersions`. The server selects one supported revision during initialization; inspect it through `getNegotiatedProtocolVersion()`. Use revision-specific features only after negotiation succeeds.
