# AJ-MCP Client Sample

This module is the starting point for a small Java 8 application using `aj-mcp-client`. The current `MyApp` class is intentionally minimal; copy one of the examples below into it and point it at an MCP server.

[中文](./README.zh-CN.md)

## Connect to a STDIO server

```java
public static void main(String[] args) throws Exception {
    try (McpClient client = McpClient.createStdioMcpClient(
            "java", "-jar", "/absolute/path/to/server.jar")) {
        client.listTools().forEach(tool -> System.out.println(tool.getName()));
    }
}
```

## Connect to the legacy SSE samples

Start the Spring or Tomcat sample, then use:

```java
McpTransport transport = new HttpMcpTransport("http://localhost:8080/sse");

try (McpClient client = McpClient.builder().transport(transport).build()) {
    client.initialize();
    System.out.println(client.listResources());
}
```

Always close the client to release its transport and any child process or pending request.
