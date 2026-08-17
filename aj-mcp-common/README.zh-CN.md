[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-common?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-common)
[![Javadoc](https://img.shields.io/badge/javadoc-1.7-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp-common)
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

# AJ-MCP Common

AJ-MCP 的公共模块，包含 Client 和 Server 共用的 JSON-RPC 消息、MCP 协议模型、内容类型、协议版本信息、传输接口、异常及 JSON 工具。

[English](./README.md)

## 主要功能

- 初始化、工具、资源、资源模板、提示词、自动补全、Ping、分页、进度和取消等请求/响应模型。
- Roots、Sampling 和 Elicitation 等客户端侧协议模型。
- 文本、图片、音频、内嵌资源和资源链接等内容模型。
- 通过 `ProtocolVersion` 集中管理 `2024-11-05`、`2025-03-26` 和 `2025-06-18` 三个协议版本。
- SDK 共用的 Jackson `JsonUtils` 工具。

使用 `aj-mcp-client` 或 `aj-mcp-server` 时会传递依赖本模块。只有在自定义传输、直接构造协议消息或仅使用协议模型时，才通常需要单独引入。

## 安装

运行环境为 Java 8 及以上版本。Maven 配置：

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-common</artifactId>
    <version>1.7</version>
</dependency>
```

## 基本用法

使用 SDK 配置好的 Jackson Mapper 序列化和解析协议对象：

```java
CallToolRequest request = new CallToolRequest(
        "weather", Collections.<String, Object>singletonMap("city", "Guangzhou"));
request.setId(1L);

String json = JsonUtils.toJson(request);
JsonNode node = JsonUtils.json2Node(json);
```

通过枚举判断协议能力，避免在业务代码中分散比较版本字符串：

```java
List<String> supported = ProtocolVersion.supportedVersions();
boolean structuredOutput = ProtocolVersion.V_2025_06_18.supportsStructuredToolOutput();
```

`JsonUtils.OBJECT_MAPPER` 在解析时会拒绝重复的 JSON 对象字段，并忽略未知字段，这也是 SDK 的默认兼容行为。

## 使用本模块的项目

- [`aj-mcp-client`](../aj-mcp-client)：客户端 API 与传输层。
- [`aj-mcp-server`](../aj-mcp-server)：能力注册、请求分发及服务端传输层。
- [用户手册](https://mcp.ajaxjs.com/)
