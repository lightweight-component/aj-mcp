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
    <version>1.4</version>
</dependency>
```

您可以通过以下链接找到最新版本：
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-client?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)

客户端 SDK 的实现由两个主要组件组成：

- **传输层**：管理子进程并处理底层消息交换。
- **MCP 客户端**：提供高层次的 API，使用传输层实现 MCP 协议。

## 设置传输层 Transport

首先，我们需要创建传输层。根据 MCP 服务器的类型，您可以选择以下两种传输方式之一：

### 标准输入输出（Stdio）传输

“Stdio” 是标准输入/输出的缩写，通常用于在程序和人之间通过命令行交互。在这里，它用于 MCP 客户端和 MCP 服务器之间的交互。通常，Stdio 用于本地应用程序，如 `*.exe` 程序或 Java Jar 程序等。

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

### SSE 传输

SSE（Server-Sent Events）传输使用 HTTP 协议实现 MCP 客户端与服务器之间的双向通信。这种传输方法特别适用于基于 Web 的应用程序。

```java
McpTransport transport=HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .logRequests(true)
        .logResponses(true)
        .build();
```

`sseUrl` 是必需的，它指定 MCP 服务器的 SSE 端点 URL。

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
| requestTimeout  | 所有请求（包括初始化和健康检查）的超时时间。默认值为 60 秒；0 表示无限等待，负值不合法。       | Duration | `Duration.ofSeconds(60)` |

请注意，在创建 McpClient 后，应立即调用 `mcpClient.initialize();`。关于初始化工作将在下一小节介绍。

```java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();

mcpClient.initialize();
```

不再使用客户端时应调用 `close()`。关闭操作会释放 HTTP/SSE 请求或 Stdio 子进程，并使尚未完成的请求以异常结束。

```java
try (IMcpClient mcpClient2 = McpClient.builder().transport(transport).build()) {
    mcpClient2.initialize();
    ...
} catch (Exception e) {
    throw new RuntimeException(e);
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
