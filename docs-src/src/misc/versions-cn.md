---
title: 版本记录
subTitle: 2024-12-05 by Frank Cheung
description: AJ MCP SDK 版本记录
date: 2022-01-05
tags:
  - 版本
layout: layouts/docs-cn.njk
---

# 版本发布说明

## 尚未发布的维护更新

- 修复 HTTP 响应泄漏，以及 SSE、Stdio 故障后的 pending request 清理。
- 完善客户端 Stdio 和服务端 SSE 的关闭生命周期。
- 修正 notification 响应行为和请求超时语义。
- 将 feature store 调整为服务器实例级，并允许包扫描跳过不可加载的可选类。
- 为 Prompt、Resource 和 ResourceTemplate 列表增加按页缓存。
- 支持字符串 JSON-RPC ID、严格初始化生命周期及按实际功能生成 capability。
- 补齐资源模板、Completion、资源订阅、取消、日志、进度、Roots、Sampling 和通用通知回调。
- 新增 opaque cursor 分页 API，同时保留页码 API 以兼容旧代码。
- 同一 SDK 支持 `2024-11-05`、`2025-03-26` 和 `2025-06-18` 协议协商；服务端配置列表按优先级排列。
- 新增 Streamable HTTP 客户端与服务端适配器；不实现已在 `2025-06-18` 删除的 JSON-RPC batch。
- 支持 Tool annotations、Progress message、Completion context 请求模型、Resource Link、结构化 Tool 输出以及 Elicitation；将
  completion context 传入服务端 provider 方法仍属于后续工作。
- 确保无参数 Tool 始终提供非 null 的 object `inputSchema`，并在扫描阶段要求每个 Tool 参数标注 `@ToolArg`。
- 按 session 与 request ID 隔离 Tool 取消状态；关闭 session 时只中断该 session 正在运行的 Tool。
- 增加严格的 initialize 字段校验，并在服务端发起 Roots 或 Sampling 请求前检查客户端 capability。
- 增加 embedded resource 的 `blob` 表达，并在序列化前校验 Tool、Prompt 与 Resource 返回集合。
- 将客户端 server-request handler 异常转换为 JSON-RPC `INTERNAL_ERROR`，不再终止消息接收循环。

Streamable HTTP 当前支持普通 JSON POST 响应和可选 GET event stream。POST `text/event-stream` 响应会先完整缓冲，尚不能增量分发；GET
stream 的就绪状态为异步。请求级 POST SSE 流式传输、断线重连/恢复以及完整的空闲 session 过期机制仍属于后续工作。

## aj-mcp-common 1.5 / aj-mcp-client 1.3 / aj-mcp-server 1.2 — 2025-06-10

- 增加[分页支持](https://modelcontextprotocol.io/specification/2024-11-05/server/utilities/pagination)。

## 1.0 — 2025-06-01

- 首次发布。
