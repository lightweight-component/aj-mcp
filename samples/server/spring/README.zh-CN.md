# 基于 Spring Boot 2.x 与 SSE 的 MCP 服务示例

本示例是在 Spring Boot 2.7 Web 应用中通过旧版 HTTP/SSE 传输运行 AJ-MCP Server。MCP 实现本身不依赖 Spring，通过 Spring MVC
Controller 完成 HTTP 适配。

[English](./README.md)

## 示例内容

- `Config`：扫描 `com.foo.myapp` 下的 `@McpService`，创建 `McpServer` 和 `ServerSse` Bean。
- `SseController`：提供 `GET /sse`，创建会话并向客户端公布消息端点。
- `MessageController`：提供 `POST /message?uuid=...`，把 JSON-RPC 消息路由回原始会话。
- `mcp/McpServerTools`、`McpServerResources`、`McpServerPrompts`：工具、资源和提示词示例。
- `Sse2Controller`：位于 `/ss2` 的普通 Spring `SseEmitter` 演示，不是 MCP 端点。

## 运行

需要 JDK 8 或以上版本及 Maven。在当前目录运行：

```bash
mvn spring-boot:run
```

也可以在 IDE 中运行 `com.foo.myapp.DemoApplication` 的 main 方法。

MCP SSE 地址：

```text
http://localhost:8080/sse
```

使用 `HttpMcpTransport` 连接。服务端会发送 `endpoint` 事件，通知客户端向 `/message?uuid=<session-id>` 发送消息：

```java
McpTransport transport = HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .build();

try (McpClient client = McpClient.builder().transport(transport).build()) {
    client.initialize();
    client.listTools().forEach(tool -> System.out.println(tool.getName()));
}
```

## 接入注意事项

- 调用 `serverSse.handle(uuid, json)` 保证响应只返回原始会话，不要用 `broadcast()` 发送请求响应。
- Servlet 请求结束时清理连接；示例已在 `finally` 中处理。
- 生产环境应在 Spring 销毁钩子中关闭 `ServerSse`，释放心跳执行器和会话。
- 本示例使用旧版 HTTP/SSE；新建 HTTP 接入且客户端兼容时，建议优先使用 `ServerStreamableHttp`。
