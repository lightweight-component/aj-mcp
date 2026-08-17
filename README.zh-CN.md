<h1 align="center">AJ-MCP</h1>
<h3 align="center">轻量级 Java MCP SDK</h3>

<div align="center" style="text-align: center;">

[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp)
![Java Version](https://img.shields.io/badge/Java-8-blue)
[![Javadoc](https://img.shields.io/badge/javadoc-1.2-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp )
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/lightweight-component/aj-mcp)
![GitHub repo size](https://img.shields.io/github/repo-size/lightweight-component/aj-mcp)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![Email](https://img.shields.io/badge/Contact--me-Email-orange.svg)](mailto:frank@ajaxjs.com)
[![QQ群](https://framework.ajaxjs.com/static/qq.svg)](https://shang.qq.com/wpa/qunwpa?idkey=3877893a4ed3a5f0be01e809e7ac120e346102bd550deb6692239bb42de38e22)
[![English](https://img.shields.io/badge/lang-English-blue)](./README.md)

</div>

<hr />

AJ-MCP 是一个轻量级 Java 模型上下文协议（MCP）SDK，为构建 MCP [客户端](./aj-mcp-client)和[服务端](./aj-mcp-server)提供简洁直接的 API，尤其适合仍需运行在 Java 8 或 Spring Boot 2.x 上的存量系统。

- 支持 Java 8 及以上版本，并支持 Spring Boot 2.x。
- 体积小，主要运行时依赖为 Jackson JSON 和 OkHttp。
- 支持 STDIO、旧版 HTTP/SSE 和 Streamable HTTP 三类传输方式。
- 支持 `2024-11-05`、`2025-03-26`、`2025-06-18` 三个 MCP 协议版本及初始化协商。
- 支持工具、资源、资源模板、提示词、自动补全、分页、订阅、取消、日志、Roots、Sampling 和 Elicitation。

请查看以下三个 MCP SDK 组件项目：

- [AJ MCP Common](./aj-mcp-common)：AJ-MCP 的通用库，包含模型上下文协议（Model Context Protocol）的实现及其他常用类。
- [AJ MCP Client](./aj-mcp-client)：MCP 客户端 SDK。
- [AJ MCP Server](./aj-mcp-server)：MCP 服务器端 SDK。
- [Samples](./samples)：客户端与服务器端的示例代码。

更多详细的介绍及文档，请查看[用户手册](https://mcp.ajaxjs.com/)以了解更多。

## 适用场景

AJ-MCP 适合希望把现有 Java 业务能力开放给 AI 客户端，或者需要在不升级整体技术栈的情况下连接 MCP 服务的应用。例如：

- 把已有 Java Service 封装成 MCP 工具、资源或提示词；
- 从 Java 桌面程序或服务端程序连接本地 STDIO MCP 子进程；
- 在 Spring Boot 2.x 或 Servlet 应用中接入 MCP；
- 同时兼容会协商不同 MCP 协议版本的客户端。

本项目有意不支持 JSON-RPC batch 批量消息。

## 模块说明

| 模块 | 功能 | 当前版本 |
| --- | --- | --- |
| [`aj-mcp-common`](./aj-mcp-common) | 公共 JSON-RPC 消息、MCP 协议模型、内容类型、版本信息及 JSON 工具 | `1.7` |
| [`aj-mcp-client`](./aj-mcp-client) | 同步客户端 API，以及 STDIO、HTTP/SSE、Streamable HTTP 传输 | `1.5` |
| [`aj-mcp-server`](./aj-mcp-server) | 基于注解的能力扫描、请求分发、错误、会话及服务端传输 | `1.4` |
| [`samples`](./samples) | STDIO、Spring Boot/SSE 和内嵌 Tomcat 示例 | — |

Client 和 Server 已经传递依赖 `aj-mcp-common`，一般应用只需引入自己使用的客户端或服务端模块。

## 快速开始

### 在 Java 8 服务端开放工具

添加服务端依赖：

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.4</version>
</dependency>
```

声明服务并通过 STDIO 启动：

```java
@McpService
public class GreetingTools {
    @Tool(description = "向用户问好")
    public String greet(@ToolArg("name") String name) {
        return "你好，" + name;
    }
}

public static void main(String[] args) {
    FeatureMgr features = new FeatureMgr();
    features.init("com.example.mcp");

    McpServer server = new McpServer();
    server.setFeatureMgr(features);
    server.setServerConfig(new ServerConfig());
    server.setTransport(new ServerStdio(server));
    server.start();
}
```

STDIO 模式下，标准输出必须只用于 JSON-RPC 消息；业务日志应写入标准错误或日志文件。工具、资源、提示词及 HTTP 传输方式请参阅[服务端 README](./aj-mcp-server/README.zh-CN.md)。

### 使用 Java 客户端连接

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-client</artifactId>
    <version>1.5</version>
</dependency>
```

```java
try (McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/path/to/mcp-server.jar")) {
    client.listTools().forEach(tool -> System.out.println(tool.getName()));
    String result = client.callTool("greet", "{\"name\":\"AJ-MCP\"}");
    System.out.println(result);
}
```

客户端使用完后应始终关闭，以释放流、HTTP 连接、等待中的请求、工作线程及可能启动的子进程。HTTP 连接和其他客户端 API 请参阅[客户端 README](./aj-mcp-client/README.zh-CN.md)。

## 从源码构建

使用 JDK 8 或 JDK 17 运行 Maven：

```bash
mvn test
```

只构建单个模块及其依赖：

```bash
mvn -pl aj-mcp-client -am test
mvn -pl aj-mcp-server -am test
```

## 为什么选择 MCP？

模型上下文协议（Model Context Protocol, MCP）为使用你喜爱的编程语言和框架，基于你的应用数据构建智能应用带来了无限可能。通过 AJ MCP 服务器，你可以轻松搭建自己的 Java 服务器，将 AI 与任意数据源或系统相连接。

无论是连接常用数据库、集成公司内部系统，还是构建全新的创新应用——你都能轻松实现！使用 AJ 实现 MCP 服务器非常简单，让你专注于创造力本身，而无需纠结底层细节。

还在等什么？获取代码，启动你的 IDE，立即开始构建属于你的 MCP 服务器吧。AI 驱动应用的未来已来，你也可以参与其中，共同塑造！

祝开发愉快！

## 相关链接

[用户手册](https://mcp.ajaxjs.com/) | [Github](https://github.com/lightweight-component/aj-mcp) | [Gitcode](https://gitcode.com/lightweight-component/aj-mcp)

## MCP 相关资源

[Awesome MCP Servers](https://mcplab.cc/zh) | [TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk) | [MCP 官方文档](https://modelcontextprotocol.io)
