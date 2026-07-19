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

## aj-mcp-common 1.5 / aj-mcp-client 1.3 / aj-mcp-server 1.2 — 2025-06-10

- 增加[分页支持](https://modelcontextprotocol.io/specification/2024-11-05/server/utilities/pagination)。

## 1.0 — 2025-06-01

- 首次发布。
