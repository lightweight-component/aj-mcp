# AJ-MCP Client 示例

专用入口：先启动 [Streamable HTTP 服务端](../server/spring-streamable-http/README.zh-CN.md)，
再在 IDEA 运行 `com.foo.StreamableHttpClientExample`。
默认地址 `http://127.0.0.1:8081/mcp`，第一个参数可覆盖。演示工具、进度、反向 roots、资源、提示词及自动关闭；
无需模型或 API Key，不上传文件。

这是使用当前 AJ-MCP SDK 的 Java 8 可运行客户端示例。`MyApp` 会初始化连接并输出协商后的协议版本、工具、提示词、资源及资源模板。

[English](./README.md)

## 运行内置客户端

`MyApp` 支持以下传输参数：

```text
stdio <command> [arguments...]
sse <http://host:port/sse>
http <http://host:port/mcp>
auto <MCP-server-URL>
```

例如，打包 STDIO 服务端示例后：

```java
McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/absolute/path/to/my-app-jar-with-dependencies.jar");
```

`auto` 优先尝试 Streamable HTTP；首次初始化遇到符合条件的端点错误时，在同一 URL 探测旧版 SSE。
不会猜测其他路径或重试业务调用，认证错误和超时也不会触发回退。
连接 Spring 或 Tomcat 兼容示例时使用 `sse http://localhost:8080/sse`。`http` 则选择当前 Streamable HTTP 客户端传输，需连接兼容的服务端。
应用会始终关闭客户端，释放子进程、事件流与等待中的请求。
