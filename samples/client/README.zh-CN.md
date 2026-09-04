# AJ-MCP Client 示例

这是使用当前 AJ-MCP SDK 的 Java 8 可运行客户端示例。`MyApp` 会初始化连接并输出协商后的协议版本、工具、提示词、资源及资源模板。

[English](./README.md)

## 运行内置客户端

`MyApp` 支持以下传输参数：

```text
stdio <command> [arguments...]
sse <http://host:port/sse>
http <http://host:port/mcp>
```

例如，打包 STDIO 服务端示例后：

```java
McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/absolute/path/to/my-app-jar-with-dependencies.jar");
```

连接 Spring 或 Tomcat 兼容示例时使用 `sse http://localhost:8080/sse`。`http` 则选择当前 Streamable HTTP 客户端传输，需连接兼容的服务端。
应用会始终关闭客户端，释放子进程、事件流与等待中的请求。
