---
title: Home
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---
# Welcome to the User Manual of AJ MCP SDK

The AJ MCP SDK is an implementation of the Model Context Protocol (MCP), an open protocol designed to enable seamless integration between Large Language Model (LLM) applications and external data sources and tools. This SDK provides the declarative APIs that make it easy for developers to implement MCP server features in your applications.

## Why AJ MCP SDK?
Compared to the other MCP SDKs, AJ MCP SDK is designed to be lightweight and easy to use. It is built on top of the Java 8, and provides a simple and intuitive API for developers to implement MCP server features. The size of the SDK is small, making it easy to integrate into your application.

For specific information about working with individual components, please see dedicated pages on Architecture, MCP Protocol, MCP Server, and MCP Client.

## What is the Model Context Protocol?

The Model Context Protocol (MCP) enables AI applications to access external tools, data sources, and prompts. It serves as a bridge between AI models and the outside world, allowing language models to interact with application-specific resources and functionality.

MCP provides several core capabilities:

- Tools: Functions that can be invoked by LLM applications
- Resources: Data that can be accessed by LLM applications
- Prompts: Templates for LLM interactions
- Notifications: Event-based communication between server and client


## Source Code
Under GNU GENERAL PUBLIC LICENSE v3.0.
 
- Github: [https://github.com/lightweight-component/aj-mcp](https://github.com/lightweight-component/aj-mcp)
- Gitcode: [https://gitcode.com/lightweight-component/aj-mcp](https://gitcode.com/lightweight-component/aj-mcp), for Chinese users faster access.


## Links

[Website](https://mcp.ajaxjs.com) | [DeepWiki](https://mcp.ajaxjs.com/docs) 

## JavaDoc

- [aj-mcp-common](https://javadoc.io/doc/com.ajaxjs/aj-mcp-common)
- [aj-mcp-client](https://javadoc.io/doc/com.ajaxjs/aj-mcp-client)
- [aj-mcp-server](https://javadoc.io/doc/com.ajaxjs/aj-mcp-server)