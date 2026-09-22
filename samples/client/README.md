# AJ-MCP Client Sample

This Java 8 sample contains a runnable `MyApp` client for the current AJ-MCP SDK. It initializes a connection and prints
the negotiated protocol version together with tools, prompts, resources, and resource templates.

[中文](./README.zh-CN.md)

## Run the included client

Start the [Streamable HTTP server](../server/spring-streamable-http/README.md), then run
`com.foo.StreamableHttpClientExample` in IDEA for a dedicated walkthrough. Its optional first argument overrides
`http://127.0.0.1:8081/mcp`. It demonstrates tools, progress, reverse roots, resources, prompts and automatic cleanup.
No model/API key is required, and no files are uploaded.

`MyApp` accepts one of these transport forms:

```text
stdio <command> [arguments...]
sse <http://host:port/sse>
http <http://host:port/mcp>
auto <MCP-server-URL>
```

For example, after packaging the STDIO server sample:

```java
McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/absolute/path/to/my-app-jar-with-dependencies.jar");
```

Use `auto` to try Streamable HTTP first and discover legacy SSE at the same URL on an eligible initialization rejection.
It does not guess another path or retry business calls. Authentication errors and timeouts do not trigger fallback.
Use `sse http://localhost:8080/sse` with the Spring or Tomcat compatibility samples. `http` selects the current
Streamable HTTP client transport for a compatible server. The application always closes its client, releasing child
processes, streams, and pending requests.
