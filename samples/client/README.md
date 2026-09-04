# AJ-MCP Client Sample

This Java 8 sample contains a runnable `MyApp` client for the current AJ-MCP SDK. It initializes a connection and prints
the negotiated protocol version together with tools, prompts, resources, and resource templates.

[中文](./README.zh-CN.md)

## Run the included client

`MyApp` accepts one of these transport forms:

```text
stdio <command> [arguments...]
sse <http://host:port/sse>
http <http://host:port/mcp>
```

For example, after packaging the STDIO server sample:

```java
McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/absolute/path/to/my-app-jar-with-dependencies.jar");
```

Use `sse http://localhost:8080/sse` with the Spring or Tomcat compatibility samples. `http` selects the current
Streamable HTTP client transport for a compatible server. The application always closes its client, releasing child
processes, streams, and pending requests.
