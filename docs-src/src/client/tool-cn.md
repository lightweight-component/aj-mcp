---
title: Working with Tools
subTitle: 2024-12-05 by Frank Cheung
description: Working with Tools
date: 2022-01-05
tags:
  - Tools
layout: layouts/docs-cn.njk
---

# 工具（Tool）使用指南

内容、资源、工具、请求参数及结果模型通过 `getMeta()` / `setMeta(Map<String, Object>)` 保留可选 `_meta`，
嵌套扩展值不会丢失；它位于相应对象内，不放在 JSON-RPC 信封顶层。
工具调用参数保留原有 `RequestMeta` API，可用 `setExtension(key, value)` 在 `progressToken` 旁添加厂商字段。
Elicitation 参数保留原有 `get_meta()` / `set_meta(...)` API。
内容与资源的 `Annotations` 支持 `audience`、`priority` 和 `lastModified`，资源链接也支持 `title`。
未设置的新字段不输出。Annotations 是不可信的展示提示，不代表授权规则。

工具 schema 在 JSON 解析与序列化过程中保留扩展关键字，包括嵌套 schema、`items`、`enum`、`oneOf`、`$ref` 和约束；联合类型、布尔属性 schema 及对象形式的 `additionalProperties` 也会保留。现有类型化 getter/setter 继续可用，`getKeywords()` 提供额外关键字的 `JsonNode` 值。保留 schema 不等于执行 JSON Schema 校验。

AJ MCP 的工具系统提供了一种结构化方式，用于定义可被客户端发现和调用的函数。每个工具都有名称、描述和一个定义其输入参数的
JSON Schema。该工具系统旨在让大语言模型（LLM）能够轻松理解可用工具及其用法。

## 列出工具

列出所有可用工具：

```java
List<ToolItem> tools = mcpClient.listTools();

assertEquals(7,tools.size());
```

`listTools()` 请求第一页（默认页），不会自动跟随 `nextCursor`。如需获取其他页，请调用 `listTools(int pageNo)`。

``` java
List<ToolItem> tools = mcpClient.listTools(1);
assertEquals(3, tools.size());
```

## 调用工具

调用某个工具：

```java
String toolExecutionResultString = mcpClient.callTool("echoString", "{\"input\": \"hi\"}");

assertEquals("hi",toolExecutionResultString);
```

当前 `callTool()` 便捷接口返回由文本内容拼接而成的 `String`，不会通过该方法暴露图片内容。工具业务错误会转换为错误消息字符串；传输错误和超时则遵循客户端的异常与超时处理逻辑。

## 通知处理

可使用 `onNotification(method, handler)` 注册 progress、日志、资源更新和列表变更通知的回调；使用
`onServerRequest(method, handler)` 响应服务端主动请求。`setRoots(...)`、`setSamplingHandler(...)` 与
`setElicitationHandler(...)` 会在 `initialize()` 前配置标准
handler。生命周期与错误处理请参阅[通知处理](handling-notifications-cn)。
