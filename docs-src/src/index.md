---
title: Home
subTitle: 2024-12-05 by Frank Cheung
description: Java 8-compatible Model Context Protocol client and server SDK
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---

# Welcome to the AJ MCP SDK User Manual

The AJ MCP SDK is an implementation of the Model Context Protocol (MCP), an open protocol designed to enable seamless integration between Large
Language Model (LLM) applications and external data sources and tools. The SDK provides declarative APIs that make it easy to implement MCP server features in Java applications.

## Why AJ MCP SDK?

Compared with other MCP SDKs, AJ MCP SDK is designed to be lightweight and easy to use. It supports Java 8 and provides a simple and
intuitive API for developers to implement MCP server features. The size of the SDK is small, making it easy to integrate into your application.

### Why Java 8?

MCP is a protocol-based framework and should therefore be more universally applicable. It should address a broader range of market demands.

Currently, the situation with developing MCP in Java is:

| Framework              | Required JDK |
|------------------------|--------------|
| Official Java MCP SDK  | JDK 17+      |
| Spring AI MCP          | JDK 17+      |
| Quarkus MCP server     | JDK 17+      |
| langchain4j-mcp client | JDK 11+      |


Given that a large number of servers still run on JDK 8, the ability to develop MCP (or MCP Server) with Java 8 is essential to ensure broad compatibility and flexibility — that’s what real freedom for MCP means.

## Learn More
For specific information about working with individual components,
please see dedicated pages on [Architecture](architecture), [Client SDK](client), [Server SDK](server).

## Source Code

Licensed under the GNU General Public License v3.0.

- GitHub: [https://github.com/lightweight-component/aj-mcp](https://github.com/lightweight-component/aj-mcp)
- GitCode: [https://gitcode.com/lightweight-component/aj-mcp](https://gitcode.com/lightweight-component/aj-mcp), which may be faster for users in China.

## Links

[Website](https://mcp.ajaxjs.com) | [DeepWiki](https://deepwiki.com/lightweight-component/aj-mcp)

## JavaDoc

- [aj-mcp-common](https://javadoc.io/doc/com.ajaxjs/aj-mcp-common)
- [aj-mcp-client](https://javadoc.io/doc/com.ajaxjs/aj-mcp-client)
- [aj-mcp-server](https://javadoc.io/doc/com.ajaxjs/aj-mcp-server)
