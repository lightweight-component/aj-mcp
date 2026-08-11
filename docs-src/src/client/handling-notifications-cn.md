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

JSON-RPC notification 不包含 `id`，服务端不得返回响应。初始化期间客户端会发送 `notifications/initialized`；Stdio 和 SSE 服务端传输都不会为该通知输出响应。

可通过 `onNotification(method, handler)` 注册进度、资源更新、日志和列表变更通知。收到对应的列表变更通知后，Resource、ResourceTemplate 和 Prompt 缓存会自动失效。

客户端还可通过 `onServerRequest(...)` 响应服务端发起的请求。便捷方法 `setRoots(...)` 和 `setSamplingHandler(...)` 应在 `initialize()` 前调用，用于配置标准的 `roots/list` 和 `sampling/createMessage` 处理器并声明相应能力。

SSE 通道关闭或失败、transport 被关闭时，所有 pending request future 都会以异常完成。Stdio 子进程意外退出时也会立即使 pending request 失败，即使 `requestTimeout` 设置为 0 也不会永久等待。
