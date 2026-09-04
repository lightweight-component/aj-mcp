[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-client?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)
[![Javadoc](https://img.shields.io/badge/javadoc-1.6-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp-client)
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Email](https://img.shields.io/badge/Contact--me-Email-orange.svg)](mailto:frank@ajaxjs.com)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![QQ群](https://framework.ajaxjs.com/static/qq.svg)](https://shang.qq.com/wpa/qunwpa?idkey=3877893a4ed3a5f0be01e809e7ac120e346102bd550deb6692239bb42de38e22)
[![English](https://img.shields.io/badge/lang-English-blue)](./README.md)

# 轻量级 Java MCP 客户端

AJ-MCP Client 是一个轻量级的 Java MCP 客户端。它为构建 MCP 客户端提供了一种简单而强大的方式，支持 Java 8 及以上版本。

## 主要功能

- STDIO 客户端传输，可启动并连接本地子进程。
- 兼容分别提供 SSE 与 POST 端点的旧版 HTTP/SSE 传输。
- 面向 MCP `2025-03-26` 和 `2025-06-18` 的 Streamable HTTP 传输。
- 自动协商 `2024-11-05`、`2025-03-26`、`2025-06-18` 三个协议版本。
- 支持工具、资源、资源模板、提示词、自动补全、分页、订阅、通知、取消和健康检查。
- 支持 Roots、Sampling 以及 `2025-06-18` Elicitation 的客户端处理器。
- 可配置请求超时；传输关闭时会及时结束所有等待中的请求。

## 源代码

[Github](https://github.com/lightweight-component/aj-mcp) | [Gitcode](https://gitcode.com/lightweight-component/aj-mcp)

## 链接

[用户手册](https://mcp.ajaxjs.com/)  | [Java 文档](https://javadoc.io/doc/com.ajaxjs/aj-mcp-client)

## 安装

运行环境为 Java8+。Maven 配置如下：

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-client</artifactId>
    <version>1.6</version>
</dependency>
```

该依赖会自动传递引入 `aj-mcp-common`。

## 快速开始：STDIO

便捷工厂方法会启动指定子进程，并在返回前完成 MCP 初始化：

```java
try(McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/path/to/server.jar")){
        for(
ToolItem tool :client.

listTools())
        System.out.

println(tool.getName() +" - "+tool.

getDescription());

String result = client.callTool(
        "echoString", "{\"input\":\"来自 Java 的问候\"}");
    System.out.

println(result);
}
```

如需自定义配置，可以分别创建传输层和客户端：

```java
McpTransport transport = StdioTransport.builder()
        .command(Arrays.asList("java", "-jar", "/path/to/server.jar"))
        .logEvents(true)
        .build();

try(
McpClient client = McpClient.builder()
        .transport(transport)
        .requestTimeout(Duration.ofSeconds(30))
        .build()){
        client.

initialize();
    client.

checkHealth();
}
```

STDIO 子进程的标准输出必须仅用于逐行 JSON-RPC 消息，日志应写入标准错误。

## 快速开始：Streamable HTTP

```java
McpTransport transport = StreamableHttpTransport.builder()
        .endpointUrl("http://localhost:8080/mcp")
        .openEventStream(true)
        .timeout(Duration.ofSeconds(30))
        .build();

try(
McpClient client = McpClient.builder()
        .transport(transport)
        .protocolVersion(ProtocolVersion.V_2025_06_18.value())
        .requestTimeout(Duration.ofSeconds(30))
        .build()){
        client.

initialize();
    System.out.

println("协商版本："+client.getNegotiatedProtocolVersion());
        client.

listResources().

forEach(resource ->System.out.

println(resource.getUri()));
        }
```

`openEventStream` 用于打开可选的长期 GET 通道，以接收服务端主动消息。传输层会保存初始化返回的 Session
ID，并在需要时发送协商后的协议版本请求头。

当前 Streamable HTTP 的限制：`text/event-stream` 的 POST 响应会在解析前先完整缓冲，尚不能增量消费；可选 GET stream
异步建立，且没有断线重连/恢复策略。请使用普通 JSON POST 响应，并且不要依赖通过 POST 响应传递的增量 progress 或服务端主动请求。

连接分别提供 SSE 和消息端点的旧服务时，使用 `HttpMcpTransport`：

```java
McpTransport transport = HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .logRequests(false)
        .logResponses(false)
        .build();
```

## 常用操作

```java
List<ToolItem> tools = client.listTools();
McpPage<ToolItem> page = client.listToolPage(null);
CallToolResult.CallToolResultDetail detail =
        client.callToolResult(new CallToolRequest("weather", "{\"city\":\"Guangzhou\"}"));

List<ResourceItem> resources = client.listResources();
GetResourceResult.ResourceResultDetail resource = client.readResource("file:///readme");

List<PromptItem> prompts = client.listPrompts();
GetPromptResult.PromptResultDetail prompt =
        client.getPrompt("review", Collections.<String, Object>singletonMap("language", "Java"));
```

服务端返回不透明 cursor 时，建议使用 `listToolPage`、`listResourcePage` 和 `listPromptPage` 等游标分页方法。旧的整数页码方法仍保留用于兼容。

## 生命周期与错误处理

- 手动构造客户端时，应先且只调用一次 `initialize()`，再发送普通请求。
- 为 `requestTimeout` 设置正数时长可以避免业务线程无限等待；设置为零表示明确关闭客户端超时。
- 使用 try-with-resources，或者在 `finally` 中调用 `close()`。关闭客户端会结束等待中的请求，并释放传输层、工作线程、HTTP
  连接和子进程。
- `callToolResult()` 会保留完整 MCP 返回值，包括结构化内容及 `isError`；`callTool()` 是只返回文本的便捷方法。
- 传输或协议错误会以运行时异常抛出；工具业务失败也可能通过 MCP Tool Result 返回。

## 协议版本

客户端通过 `protocolVersion` 声明首选版本，并通过 `supportedProtocolVersions` 提供可接受版本列表。服务端在初始化期间选择双方都支持的版本，之后可通过
`getNegotiatedProtocolVersion()` 查看。只有协商成功后才能安全使用对应版本的特性。
