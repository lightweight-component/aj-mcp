---
title: 用户手册 首页
subTitle: 2024-12-05 by Frank Cheung
description: 用户手册 首页
date: 2022-01-05
tags:
  - 用户手册
layout: layouts/docs-cn.njk
---

# 欢迎使用 AJ MCP SDK 用户手册

AJ MCP SDK 是 **模型上下文协议**（MCP）的一个实现。MCP 是一种开放协议，旨在实现大型语言模型（LLM）应用与外部数据源和工具之间的无缝集成。本 SDK 提供了声明式的 API，使开发者能够轻松地在自己的应用程序中实现 MCP 服务器功能。

## 为什么选择 AJ MCP SDK？

与其他 MCP SDK 相比，AJ MCP SDK 被设计得更轻量、更易于使用。它基于 Java 8 构建，为开发者提供了简单直观的 API 来实现 MCP 服务器功能。SDK 的体积小巧，便于集成到你的应用程序中。

### 为什么还在使用 Java 8？

MCP 是一个基于协议的框架，因此应具有更强的通用性，满足更广泛的市场需求。

目前 Java 领域中开发 MCP 的情况如下：

| 框架名称                | 所需 JDK 版本 |
|---------------------|-----------|
| 官方 Java MCP SDK     | jdk17+    |
| Spring AI MCP       | jdk17+    |
| Quarkus MCP 服务器     | jdk17+    |
| langchain4j-mcp 客户端 | jdk11+    |

考虑到仍有大量服务器运行在 JDK 8 上，使用 Java 8 开发 MCP（或 MCP 服务器）对于确保广泛兼容性和灵活性至关重要——这才是 MCP 真正意义上的自由。

## 更多信息

如需了解各组件的具体使用方法，请参阅以下页面：

- [架构说明](architecture)
- [客户端 SDK](client)
- [服务端 SDK](server)

## 源代码

本项目采用 GNU GENERAL PUBLIC LICENSE v3.0 协议开源。

- GitHub 地址：[https://github.com/lightweight-component/aj-mcp](https://github.com/lightweight-component/aj-mcp)
- GitCode 地址（适合中国用户快速访问）：[https://gitcode.com/lightweight-component/aj-mcp](https://gitcode.com/lightweight-component/aj-mcp)

## 相关链接

[官网](https://mcp.ajaxjs.com) | [DeepWiki 页面](https://deepwiki.com/lightweight-component/aj-mcp)

## JavaDoc 文档

- [aj-mcp-common](https://javadoc.io/doc/com.ajaxjs/aj-mcp-common)
- [aj-mcp-client](https://javadoc.io/doc/com.ajaxjs/aj-mcp-client)
- [aj-mcp-server](https://javadoc.io/doc/com.ajaxjs/aj-mcp-server)