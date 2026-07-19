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

当前客户端能够识别 `notifications/message`，并把参数写入应用日志。进度、资源变更、工具列表变更和提示列表变更等通知尚无公开的回调注册 API，因此不能依赖通知自动使列表缓存失效。

SSE 通道关闭或失败、transport 被关闭时，所有 pending request future 都会以异常完成。Stdio 子进程意外退出时也会立即使 pending request 失败，即使 `requestTimeout` 设置为 0 也不会永久等待。
