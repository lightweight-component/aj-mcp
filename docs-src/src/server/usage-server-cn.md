---
title: MCP Server SDK 用法指南
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK 用法指南
date: 2022-01-05
tags:
  - MCP Server SDK 用法指南
layout: layouts/docs-cn.njk
---

# MCP 服务器 SDK 使用说明

### 客户端 roots 变更回调

注册回调以接收 `notifications/roots/list_changed`：

```java
server.setRootsChangedHandler(sessionId -> {
    // Invalidate application-owned cached roots for this session.
    rootsCache.remove(sessionId);
});
```

这里的 `rootsCache` 是应用自行维护、以会话 ID 为键的线程安全缓存。该回调适用于 STDIO、旧 SSE 和 Streamable HTTP，兼容三个已支持的协议版本。只有声明了 `capabilities.roots.listChanged: true` 的客户端才会触发回调。通知不包含新的 roots 内容，需要时请另行调用 `server.listRoots(sessionId, timeout)` 查询。

回调在接收线程执行，应快速返回。不要在回调中直接阻塞调用 `listRoots`：接收线程可能还需要处理它的响应，应使用业务自行管理的执行器异步刷新。回调需保证线程安全，异步刷新需处理会话关闭和超时，执行器由应用负责关闭。回调抛出的运行时异常只会被记录，不会产生 JSON-RPC 响应或中断接收流程。传入 `null` 可注销回调。SDK 不创建额外执行器，也不会自动刷新 roots。

### 旧 SSE 的 Origin 校验

HTTP 适配层应在打开 SSE 流或处理 POST 消息之前调用 `serverSse.isOriginAllowed(request.getHeader("Origin"))`，返回 false 时响应 HTTP 403。内置 Spring 和 Tomcat 适配器已在两个入口加入检查。不携带 Origin 的原生客户端仍可连接；携带 Origin 时必须精确匹配 `ServerConfig.allowedOrigins`。空字符串和不透明来源 `null` 会被拒绝，不支持通配符。

```java
config.setAllowedOrigins(java.util.Collections.singletonList("https://app.example.com"));
```

Origin 校验不代替身份认证，也不会自动配置 CORS 响应头。

## MCP 服务器 SDK 安装

添加如下依赖以构建 MCP 服务器：

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.6</version>
</dependency>
```

可在此处查看最新版本：
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-server?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-server)

服务端模块包含：

- `McpServer` 核心处理引擎
- 基于注解的功能发现管理器 `FeatureMgr`
- `@Tool`、`@Resource`、`@Prompt` 等注解
- STDIO、旧版 HTTP/SSE 与 Streamable HTTP 传输实现

## 创建服务器

要创建 MCP 服务器，需要以下步骤：

1. 定义服务类：创建带有 `@McpService` 注解的类
2. 注解方法：使用 `@Tool`、`@Prompt` 或 `@Resource` 等注解标记方法
3. 初始化功能管理器：扫描包以发现注解
4. 配置传输层：设置 STDIO、旧版 HTTP/SSE 或 Streamable HTTP，并配置相关服务器参数
5. 启动服务器：调用 `server.start()`

## 创建 MCP 服务类

AJ-MCP 通过注解扫描自动发现、注册和管理 MCP 功能（工具、资源、提示）。开发者只需在带有 `@McpService` 注解的类中，使用 `@Tool`、
`@Resource` 或 `@Prompt` 注解标记方法，即可暴露相应功能。

```java

@McpService
public class MyServerFeatures {
    @Tool(description = "回显字符串")
    public String echoString(@ToolArg(description = "输入字符串") String input) {
        return input;
    }

    @Prompt(description = "基础问候提示")
    public PromptMessage greeting(@PromptArg(description = "姓名") String name) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello " + name));
        return message;
    }
}
```

## 服务器功能管理

每个服务器使用独立的 `FeatureMgr` 实例负责包扫描、注解处理与功能存储，因此同一 JVM 中的多个服务器不会共享工具、资源或提示。

### 注解体系

注解体系围绕几个核心注解展开，用于标记类和方法以供 MCP 识别和暴露：

| 注解          | 目标 | 作用描述          |
|-------------|----|---------------|
| @McpService | 类  | 标记服务发现类       |
| @Tool       | 方法 | 将方法暴露为 MCP 工具 |
| @ToolArg    | 参数 | 定义工具方法参数      |
| @Resource   | 方法 | 将方法暴露为 MCP 资源 |
| @Prompt     | 方法 | 将方法暴露为 MCP 提示 |
| @PromptArg  | 参数 | 定义提示方法参数      |

调用 `FeatureMgr.init()` 会扫描指定包，并注册 `@McpService` 类中的 `@Tool`、`@Resource` 和 `@Prompt`
方法。单个类无法加载时会记录日志并跳过，不会中断整个扫描过程。

### 初始化功能管理器

`FeatureMgr.init()` 方法负责整个注解发现流程。它首先扫描指定包下带有 `@McpService` 注解的类。

```java
FeatureMgr mgr = new FeatureMgr();
mgr.

init("com.foo.myproduct");
```

## 服务器配置

包扫描初始化功能管理器后，可进行服务器配置，包括：

- 创建服务器实例并设置传输层
- 配置服务器名称与版本号
- 设置分页大小和协议版本等参数

服务器配置由 `ServerConfig` 类管理，包含服务端元数据。初始化时还会进行协议版本协商，返回所支持的最高版本，或与客户端请求一致的版本。

```java
FeatureMgr mgr = new FeatureMgr();
mgr.

init("com.foo.myproduct");

McpServer server = new McpServer();
server.

setFeatureMgr(mgr);
server.

setTransport(new ServerStdio(server));

ServerConfig serverConfig = new ServerConfig();
serverConfig.

setName("MY_MCP_Server");
serverConfig.

setVersion("1.0");
server.

setServerConfig(serverConfig);

server.

start();
```

## 传输与生命周期规则

- `ServerStdio` 每行交换一个 JSON-RPC 消息，`System.out` 必须仅用于协议输出。
- `ServerSse` 是旧版双端点适配器：用 `openSession(...)` 建立会话，把 POST 消息交给 `handle(sessionId, body)`
  ，断开连接时移除会话，并在应用关闭时关闭适配器。
- `ServerStreamableHttp` 使用单一端点：`POST` 委托给 `post(body, headers)`，可选 `GET` event stream 委托给
  `openEventStream(sessionId, writer, headers)`，`DELETE` 委托给 `delete(sessionId, headers)`。将返回 `HttpResult`
  的状态码、响应头、content type 和正文复制到框架响应中。

初始化会协商支持的版本，并创建 Streamable HTTP session。使用 MCP `2025-06-18` 时，后续 Streamable HTTP 请求必须带上协商后的
`MCP-Protocol-Version` header。`strictLifecycle` 默认开启，普通请求必须在 `initialize` 和 `notifications/initialized`
之后发送。本项目有意不支持 JSON-RPC batch。

GET 响应须异步保持打开，控制器返回前 flush 响应头。在框架完成、错误或超时回调中调用
`closeEventStream(sessionId, writer)`，由适配器关闭注册的 Writer。GET 每 15 秒发送心跳；
`ServerConfig.sessionIdleTimeout` 默认 30 分钟，也会清理未打开 GET 的会话。执行中的 POST 不会过期，心跳本身不延长空闲时间。
关闭传输层会移除全部会话。POST 仍返回 JSON，客户端响应和通知以无正文的 202 接收。

反向调用传入 null/零超时时，使用 `clientRequestTimeout` 的有限默认值 60 秒。整数工具参数的小数或溢出返回
`INVALID_PARAMS`，日志阈值按会话过滤。补全方法可采用 `(String value, Map<String, String> arguments)`，
读取 2025-06-18 `context.arguments` 的只读视图；原单 String 参数签名继续支持。

`@Tool(title = "天气")` 在 MCP 2025-03-26 及以后输出 `annotations.title`，2025-06-18 还输出工具顶层 `title`；
2024-11-05 不输出这两个字段。
进度文字可通过 `server.sendProgress(sessionId, token, progress, total, "处理中")` 发送，
当前请求线程内也可使用 `server.sendProgress(token, progress, total, "处理中")`。
对 2024-11-05 会自动省略 `message`；原有重载保持兼容。

STDIO 使用 UTF-8。构造 `ServerStdio` 前可配置 `stdioWorkers`（默认 16）和 `stdioQueueCapacity`（默认 256）。
队列满时返回繁忙错误，握手、ping、取消和反向响应仍可处理。

`ServerConfig.title` 可配置服务端展示名，仅在 2025-06-18 初始化响应中输出。
客户端可用 `McpClient.builder().clientTitle(...)`，请求 2025-06-18 时携带展示名。
`StructuredToolResult.setMeta(...)` 会将应用元数据传入工具结果的 `_meta`，不展开到顶层。
Streamable HTTP DELETE 在移除会话前校验版本头，初始化也拒绝显式不支持的版本头。
旧 HTTP/SSE 的后续 POST（包括 initialized 通知）会携带协商后的版本头。

可运行的新版示例位于源码 `samples/server/spring-streamable-http`：先运行 `DemoApplication`，
再运行 `samples/client` 的 `com.foo.StreamableHttpClientExample`，默认端点为
`http://127.0.0.1:8081/mcp`。兼容 Java 8，演示异步 GET、POST/DELETE、进度及反向 roots，
附中英文 README 和随机端口集成测试。
