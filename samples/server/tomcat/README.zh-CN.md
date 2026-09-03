# 基于内嵌 Tomcat 的 MCP SSE 服务示例

这是一个不依赖 Spring、使用 Java 8 和内嵌 Tomcat 8.5 的独立 JAR 示例，通过旧版 HTTP/SSE 传输提供 MCP 服务。

[English](./README.md)

示例使用两个 Servlet 适配 AJ-MCP 的 `ServerSse`：

- `GET /sse` 打开事件流，并公布当前会话的消息端点。
- `POST /message?uuid=...` 接收 JSON-RPC 消息，只把响应发送给对应会话。

工具、资源和提示词会从 `com.foo.myapp` 包中扫描注册。

## 构建与运行

需要 JDK 8 或以上版本及 Maven：

```bash
mvn package
java -jar target/mcp-demo-tomcat-1.0.jar
```

如果当前 Maven 环境生成的 shaded JAR 名称不同，请运行 `target/` 下非 original 的 JAR。

MCP 地址：

```text
http://localhost:8080/sse
```

使用旧版客户端传输连接：

```java
McpTransport transport = HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .build();

try (McpClient client = McpClient.builder().transport(transport).build()) {
    client.initialize();
    System.out.println(client.listTools());
}
```

## 实现结构

`StandaloneTomcat` 创建 `FeatureMgr`、`McpServer` 和 `ServerSse`，然后以编程方式注册 `SseServlet` 与 `MessageServlet`，无需
Spring 容器或 `web.xml`。

生产环境还应增加关闭钩子以关闭 `ServerSse`，在 HTTP 层校验请求大小和身份认证，并根据 SSE 连接的预期生命周期配置超时。
