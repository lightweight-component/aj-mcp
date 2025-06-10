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

AJ MCP 的工具系统提供了一种结构化方式，用于定义可被客户端发现和调用的函数。每个工具都有名称、描述和一个定义其输入参数的 JSON Schema。该工具系统旨在让大语言模型（LLM）能够轻松理解可用工具及其用法。

## 列出工具

列出所有可用工具：

```java
List<ToolItem> tools = mcpClient.listTools();
assertEquals(7, tools.size());
```
此方法将跨多个页面获取所有的工具，并没有分页。如果你需要对分页进行更精细的控制，可以改用重载的`listTools(int pageNo)`方法。

``` java
List<ToolItem> tools = mcpClient.listTools(1);
assertEquals(3, tools.size());
```
## 调用工具

调用某个工具：

```java
String toolExecutionResultString = mcpClient.callTool("echoString", "{\"input\": \"hi\"}");
assertEquals("hi", toolExecutionResultString);
```

`callTool()` 方法返回一个 CallToolResult，包含工具的响应内容，可能是文本、图片或其他内容类型。

<!--
对于支持进度通知的工具，可以在请求元数据中提供 ProgressToken，并注册通知处理器来接收进度更新。

## 通知处理

客户端可通过 OnNotification 方法接收来自服务器的通知：

client.OnNotification(func(notification mcp.JSONRPCNotification) {
    // 处理通知
    if notification.Method == "notifications/progress" {
        // 处理进度通知
    }
})

你可以注册多个通知处理器，按注册顺序依次调用。

详细通知处理机制请参阅通知处理文档。

## 错误处理

所有与服务器通信的客户端方法均可能返回错误，包括：

- 传输错误：传输层发生的问题
- 协议错误：服务器返回的错误
- 解析错误：解析响应时发生的错误

建议在每次客户端操作后检查错误：

result, err := client.SomeOperation(ctx, request)
if err != nil {
    // 处理错误
    return err
}
// 处理结果

-->