[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-server?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-server)
[![Javadoc](https://img.shields.io/badge/javadoc-1.5-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp-server)
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Email](https://img.shields.io/badge/Contact--me-Email-orange.svg)](mailto:frank@ajaxjs.com)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![QQ群](https://framework.ajaxjs.com/static/qq.svg)](https://shang.qq.com/wpa/qunwpa?idkey=3877893a4ed3a5f0be01e809e7ac120e346102bd550deb6692239bb42de38e22)
[![English](https://img.shields.io/badge/lang-English-blue)](./README.md)

# 轻量级 Java MCP 服务器

AJ-MCP Server 是一个轻量级的 Java MCP 服务器。它为构建 MCP 服务器提供了简单而强大的方式，支持 Java 8 及以上版本。

它可以通过注解把普通 Java 方法注册为 MCP 工具、资源、资源模板、提示词和自动补全能力，并通过 STDIO、旧版 HTTP/SSE 或
Streamable HTTP 对外提供服务。

## 主要功能

- 兼容 Java 8 和 Spring Boot 2.x。
- 扫描标记了 `@McpService` 的业务类。
- 通过注解声明工具、资源、资源模板、提示词和参数自动补全。
- 自动生成工具输入 JSON Schema，无参数工具也会生成非空对象 schema。
- 在初始化期间协商 `2024-11-05`、`2025-03-26` 和 `2025-06-18` 协议版本。
- 支持分页、资源订阅、取消、进度/日志通知、Roots、Sampling 和 Elicitation。
- STDIO 与 HTTP 传输具有按会话路由和关闭生命周期。
- 对错误方法、参数和能力调用返回结构化 JSON-RPC 错误。

## 源代码

[Github](https://github.com/lightweight-component/aj-mcp) | [Gitcode](https://gitcode.com/lightweight-component/aj-mcp)

## 链接

[教程](https://mcp.ajaxjs.com/) | [Java 文档](https://javadoc.io/doc/com.ajaxjs/aj-mcp-server)

## 安装

运行环境：Java 8 及以上。Maven 配置如下：

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.5</version>
</dependency>
```

该依赖会自动传递引入 `aj-mcp-common`。

## 快速开始：声明 MCP 能力

把 MCP 方法放在带有公共无参构造方法，并标记了 `@McpService` 的类中：

```java

@McpService
public class DemoFeatures {
    @Tool(description = "两个整数相加")
    public String add(@ToolArg("a") Integer a,
                      @ToolArg("b") Integer b) {
        return String.valueOf(a + b);
    }

    @Tool(description = "返回服务器时间")
    public String serverTime() {
        return new Date().toString();
    }

    @Resource(uri = "config:///application", mimeType = "text/plain",
            description = "当前应用配置")
    public ResourceContentText configuration() {
        ResourceContentText content = new ResourceContentText();
        content.setUri("config:///application");
        content.setMimeType("text/plain");
        content.setText("mode=production");
        return content;
    }

    @Prompt(description = "生成代码审查提示词")
    public PromptMessage review(@PromptArg("language") String language) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("请审查这段 " + language + " 代码"));
        return message;
    }
}
```

工具方法的每个 Java 参数都必须标记 `@ToolArg`。参数默认必填，允许省略时设置 `required = false`。无参数工具是合法的，并会发布为空对象输入
schema。

## 快速开始：STDIO 服务

```java
public static void main(String[] args) {
    FeatureMgr features = new FeatureMgr();
    features.init("com.example.mcp");

    ServerConfig config = new ServerConfig();
    config.setName("example-server");
    config.setVersion("1.0.0");

    McpServer server = new McpServer();
    server.setFeatureMgr(features);
    server.setServerConfig(config);
    server.setTransport(new ServerStdio(server));
    server.start();
}
```

STDIO 每行传输一个 JSON-RPC 消息。不要向 `System.out` 输出业务内容，否则会破坏协议流；日志应配置到标准错误或文件。
`server.start()` 会阻塞，直到标准输入关闭或传输停止。

## 旧版 HTTP/SSE

`ServerSse` 是旧版 MCP HTTP/SSE 的框架无关适配器。Servlet 或 Controller 需要：

1. 提供 `Content-Type: text/event-stream` 的 `GET` 端点；
2. 生成客户端/会话 ID，并调用 `openSession(id, writer, messageEndpoint)`；
3. 提供服务端公布的 `POST` 消息端点，并把 JSON 请求体交给 `handle(id, body)`；
4. 连接断开时调用 `removeConnection(id)`；
5. 应用停止时关闭传输层。

```java
McpServer server = new McpServer();
server.

setFeatureMgr(features);
server.

setServerConfig(config);

ServerSse transport = new ServerSse(server);
server.

setTransport(transport);
server.

start();
```

Controller 和 Servlet 接线方式请参考 [Spring Boot](../samples/server/spring) 与[内嵌 Tomcat](../samples/server/tomcat)
示例。

## Streamable HTTP

`ServerStreamableHttp` 同样不绑定具体 Servlet 框架。在同一个 MCP 端点上分别委托：

- `POST` 请求调用 `post(body, headers)`；
- 可选的 `GET` 事件流调用 `openEventStream(sessionId, writer, headers)`；
- `DELETE` 请求调用 `delete(sessionId, headers)`。

把 `HttpResult` 中的状态码、响应头、内容类型和正文复制到框架响应。初始化会创建会话并返回 `Mcp-Session-Id`。使用 `2025-06-18`
时，后续请求必须携带协商后的 `MCP-Protocol-Version` 请求头。浏览器 Origin 白名单通过 `ServerConfig.allowedOrigins`
配置；空列表会拒绝所有携带 `Origin` 的请求。

本项目有意不支持 JSON-RPC batch 请求。

当前 Streamable HTTP 的限制：支持普通 JSON POST 响应和可选 GET event stream，但服务端尚不能生成请求级的 POST SSE 响应。未打开
GET stream 的 session 应通过 `DELETE` 端点终止；当前未实现自动空闲 session 过期。

## 配置

`ServerConfig` 的主要属性：

| 属性                 | 含义                                 | 默认值     |
|--------------------|------------------------------------|---------|
| `name`、`version`   | 初始化时返回的服务身份                        | 未设置     |
| `pageSize`         | 每页工具、资源或提示词数量                      | `3`     |
| `protocolVersions` | 支持的协议版本，新版本优先                      | 所有已实现版本 |
| `strictLifecycle`  | 普通请求前是否强制 initialize → initialized | `true`  |
| `allowedOrigins`   | Streamable HTTP 接受的浏览器 Origin      | 空列表     |

每个服务创建一个 `FeatureMgr`，并通过 `setFeatureMgr` 赋给 `McpServer`。能力存储是实例级的，扫描一个服务不会自动填充另一个服务。

## 关闭服务

应用停止时应关闭已配置的传输层。关闭会清理会话、停止心跳/执行器线程、中断适用的 STDIO 处理，并释放 Writer。框架应用可放在
`@PreDestroy`、Servlet destroy 或对应的销毁钩子中执行。
