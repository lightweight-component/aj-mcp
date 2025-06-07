---
title: 架构原理
subTitle: 2024-12-05 by Frank Cheung
description: 架构原理
date: 2022-01-05
tags:
  - Architecture
  - 架构原理
layout: layouts/docs-cn.njk
---

# MCP 客户端与服务器架构

## 什么是模型上下文协议（MCP）？

[模型上下文协议（Model Context Protocol, MCP）](https://modelcontextprotocol.io/introduction) 使 AI 应用能够访问外部工具、数据源和提示信息。它在 AI 模型与外部世界之间架起了一座桥梁，使语言模型能够与应用程序特定的资源和功能进行交互。

MCP 提供了以下核心能力：

- **工具（Tools）**：LLM 应用程序可以调用的功能
- **资源（Resources）**：LLM 应用程序可以访问的数据
- **提示词（Prompts）**：用于 LLM 交互的模板
- **通知（Notifications）**：服务器与客户端之间的基于事件的通信

简而言之，MCP 为 LLM 应用提供了丰富多样的上下文，使其能够与外部资源和工具进行交互。

## 架构说明

AJ MCP SDK 的结构围绕几个核心组件构建，这些组件协同工作以实现 MCP 规范：

![架构图](/asset/imgs/a-1.jpg)

该架构采用客户端-服务器模型，并支持灵活的传输方式。客户端可以通过 HTTP（带 Server-Sent Events）或标准 I/O 管道使用 JSON-RPC 与服务器进行交互。

# Stdio 模式 vs SSE 模式对比

| 特性         | Stdio 模式 | SSE 模式 |
|------------|----------|--------|
| **部署方式**   | 本地子进程    | 独立服务器  |
| **适用场景**   | 本地开发     | 分布式环境  |
| **配置复杂度**  | 复杂       | 简单     |
| **多客户端支持** | 不支持      | 支持     |
| **网络要求**   | 无        | 需要网络连接 |