# AJ-MCP Client 示例

本模块是使用 `aj-mcp-client` 创建 Java 8 小型应用的起点。当前 `MyApp` 类有意保持最小内容，可以把下面任一示例复制进去，并改为实际 MCP 服务地址。

[English](./README.md)

## 连接 STDIO 服务

```java
public static void main(String[] args) throws Exception {
    try (McpClient client = McpClient.createStdioMcpClient(
            "java", "-jar", "/absolute/path/to/server.jar")) {
        client.listTools().forEach(tool -> System.out.println(tool.getName()));
    }
}
```

## 连接旧版 SSE 示例

先启动 Spring 或 Tomcat 示例，然后使用：

```java
McpTransport transport = new HttpMcpTransport("http://localhost:8080/sse");

try (McpClient client = McpClient.builder().transport(transport).build()) {
    client.initialize();
    System.out.println(client.listResources());
}
```

客户端使用完成后必须关闭，以释放传输层、子进程及等待中的请求。
