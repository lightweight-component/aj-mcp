---
title: 通知处理
subTitle: 2024-12-05 by Frank Cheung
description: AJ MCP 客户端的通知行为
date: 2022-01-05
tags:
  - 通知
layout: layouts/docs-cn.njk
---

# 通知处理

JSON-RPC notification 不包含 `id`，服务端不得返回响应。初始化期间客户端会发送 `notifications/initialized`；STDIO、旧版
HTTP/SSE 和 Streamable HTTP 服务端传输都不会为该通知输出响应。

可通过 `onNotification(method, handler)` 注册进度、资源更新、日志和列表变更通知。收到对应的列表变更通知后，Resource、ResourceTemplate
和 Prompt 缓存会自动失效。

客户端还可通过 `onServerRequest(...)` 响应服务端发起的请求。便捷方法 `setRoots(...)`、`setSamplingHandler(...)` 和
`setElicitationHandler(...)` 应在 `initialize()` 前调用，用于配置标准的 `roots/list`、`sampling/createMessage` 和
`elicitation/create` 处理器并声明相应能力；Elicitation 需要 MCP `2025-06-18`。

如果 server-request handler 抛出运行时异常，客户端会在公共 transport 边界捕获异常，并使用原始请求 ID 返回 JSON-RPC
`INTERNAL_ERROR`（`-32603`）。异常会被记录，但不会终止 STDIO、旧版 SSE 或 Streamable HTTP 接收循环。handler 返回 `null`
表示没有可用结果，此时返回 `METHOD_NOT_FOUND`。

旧版 SSE 通道关闭或失败、transport 被关闭时，所有 pending request future 都会以异常完成。STDIO 子进程意外退出时也会立即使
pending request 失败，即使 `requestTimeout` 设置为 0 也不会永久等待。当前 Streamable HTTP 会把 GET event stream
的失败也视为传输失败，且还没有断线重连/恢复策略。

服务端的取消状态同时以 session 和 JSON-RPC request ID 隔离。两个客户端可以安全地使用相同 request ID；取消其中一个 session
的请求不会中断另一个 session。关闭 session 时也只会中断该 session 正在运行的工具。
