---
title: MCP Client SDK 安装与配置
subTitle: 2024-12-05 by Frank Cheung
description: MCP Client SDK 安装与配置
date: 2022-01-05
tags:
  - 安装与配置
layout: layouts/docs-cn.njk
---

# MCP 客户端 SDK 设置

## 安装依赖

我们需要使用 AJ MCP SDK 来进行 API 请求。安装依赖如下：

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-client</artifactId>
    <version>1.7</version>
</dependency>
```

您可以通过以下链接找到最新版本：
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-client?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)

客户端 SDK 的实现由两个主要组件组成：

- **传输层**：管理子进程并处理底层消息交换。
- **MCP 客户端**：提供高层次的 API，使用传输层实现 MCP 协议。

## 设置传输层 Transport

完整的 Streamable HTTP 演示：启动源码中的 `samples/server/spring-streamable-http`，
再运行 `samples/client` 入口 `com.foo.StreamableHttpClientExample`。
示例等待 GET 就绪，调用工具、接收进度、响应 roots、读取资源、获取提示词并关闭会话。

首先创建与 MCP 服务端匹配的传输层。客户端支持 STDIO、旧版双端点 HTTP/SSE 和 Streamable HTTP 三类传输。

### 自动识别 HTTP 传输

不确定服务端使用哪一种 HTTP 传输时，可使用 `AutoHttpTransport`：

```java
McpTransport transport = new AutoHttpTransport("http://localhost:8080/mcp");
McpClient client = McpClient.builder().transport(transport).build();
client.initialize();
// 使用完成后调用 client.close()。
```

首次 POST 尝试 Streamable HTTP；遇到 400/404/405/415 时，对**同一 URL** 发起 GET，
等待 SSE `endpoint` 事件提供旧版 POST 地址，不猜测 `/sse` 路径。
认证失败、限流、服务端错误、网络超时及 JSON-RPC 错误不会触发回退。
初始化完成后的业务调用不会切换传输；Streamable HTTP 会话 404 仍走会话重建。
协议版本单独协商，不强制降为 2024-11-05，并遵守客户端配置的受支持版本列表。

Builder 可配置 `endpointUrl`、`timeout`、`requestHeaders` 和 `openEventStream`。
最后一项仅控制新版可选 GET；旧版 SSE 必须打开 GET。回退保留自定义请求头；
为防止凭据泄露，旧版 endpoint 必须与服务端 URL 同源。`isLegacySse()` 可查看是否选择了旧版实现。
原有 `StreamableHttpTransport` 和 `HttpMcpTransport` 仍可显式使用，不会自动切换。

### 标准输入输出（Stdio）示例

“Stdio” 是标准输入/输出的缩写，通常用于在程序和人之间通过命令行交互。在这里，它用于 MCP 客户端和 MCP 服务器之间的交互。通常，Stdio
用于本地应用程序，如 `*.exe` 程序或 Java Jar 程序等。

```java
// MCP 服务器是一个 Java 程序，使用标准输入输出运行。
McpTransport transport = StdioTransport.builder()
                .command(Arrays.asList("java", "-jar", "C:\\app\\my-app-jar-with-dependencies.jar"))
                .logEvents(true)
                .build();
```

以下是一个 `.exe` 程序的示例：

```java
// MCP 服务器是一个可执行程序，使用标准输入输出运行。
McpTransport transport = StdioTransport.builder()
                .command(Arrays.asList("C:\\app\\my-app.exe", "-token", "dd4df2sx32ds"))
                .logEvents(true)
                .build();
```

调试时可将 `logEvents` 设置为 `true`，以记录发出的协议消息。传输层也会持续消费子进程的 stderr，避免错误输出管道写满后阻塞子进程。

### 旧版 HTTP/SSE 传输

旧版传输使用 SSE 端点承载服务端到客户端的消息，并使用服务端公布的 POST 端点承载客户端请求，适用于需要对接旧 MCP 服务的场景。

```java
McpTransport transport = HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .logRequests(true)
        .logResponses(true)
        .build();
```

`sseUrl` 是必需的，它指定 MCP 服务器的 SSE 端点 URL。

### Streamable HTTP（2025-03-26 / 2025-06-18）

较新的协议版本使用单一 HTTP 端点：

```java
StreamableHttpTransport transport = StreamableHttpTransport.builder()
        .endpointUrl("http://localhost:8080/mcp")
        .openEventStream(true)
        .build();

McpClient client = McpClient.builder()
        .transport(transport)
        .protocolVersion("2025-06-18")
        .build();
client.

initialize();
```

如需 OAuth Bearer token，可通过 `requestHeaders` 传入 `Authorization`。SDK 会保存服务端返回的 session ID，并在后续请求中自动加入协商后的
`MCP-Protocol-Version`。

如果客户端声明了 Roots、Sampling 或 Elicitation handler，请设置 `openEventStream(true)`；服务端主动请求通过可选的 GET event
stream 到达客户端。该流会在初始化后异步打开。

POST SSE 响应按事件增量处理，包括最终响应前的 progress 和反向请求。GET 故障不会终止独立 POST 请求。
依赖 GET 的业务应等待 `transport.getEventStreamReady().get(5, TimeUnit.SECONDS)`，并通过
`isEventStreamOpen()` / `getEventStreamFailure()` 查询状态。GET 断线按指数退避重试最多五次（200 毫秒至 5 秒），
有事件 ID 时携带 `Last-Event-ID`，重放能力由服务端提供；HTTP 4xx 停止重试。关闭时尽力发送 DELETE，最多等待两秒。
STDIO 消息固定使用 UTF-8。

## MCP 客户端

MCP 客户端充当本地应用程序与远程工具实现之间的桥梁。

```java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(transport)
        .build();
```

通常我们会填写 `clientName` 和 `clientVersion` 属性：

- `clientName` 属性用于向 MCP 服务器标识客户端。
- `clientVersion` 属性用于指示客户端的版本。

所有属性如下所示：

| 属性              | 说明                                                      | 值类型      | 示例值                      |
|-----------------|---------------------------------------------------------|----------|--------------------------|
| clientName      | 设置客户端在初始化消息中向 MCP 服务器标识自己的名称。                           | String   | myapp/foo-app            |
| clientVersion   | 设置客户端在初始化消息中向 MCP 服务器标识自己的版本字符串。默认值为 "1.0"。             | String   | 1.0/2.1.2                |
| protocolVersion | 设置客户端在初始化消息中声明的协议版本。当前默认值为 "2024-11-05"，但在后续版本中可能会有所更改。 | String   | 2024-11-05               |
| requestTimeout  | 请求、初始化和健康检查的超时时间。null 或零使用有限的 60 秒默认值，负值不合法。 | Duration | `Duration.ofSeconds(60)` |

请注意，在创建 McpClient 后，应立即调用 `mcpClient.initialize();`。关于初始化工作将在下一小节介绍。

```java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();

mcpClient.

initialize();
```

不再使用客户端时应调用 `close()`。关闭操作会释放 HTTP/SSE 请求或 Stdio 子进程，并使尚未完成的请求以异常结束。

```java
try(IMcpClient mcpClient2 = McpClient.builder().transport(transport).build()){
        mcpClient2.

initialize();
    ...
            }catch(
Exception e){
        throw new

RuntimeException(e);
}
```

MCP 客户端遵循分层架构，接口定义与实现之间有清晰的分离。客户端依赖传输层与服务器进行实际通信，并抽象通信细节以支持不同的传输机制。
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
