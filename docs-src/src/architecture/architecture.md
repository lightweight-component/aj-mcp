---
title: Architecture
subTitle: 2024-12-05 by Frank Cheung
description: Architecture
date: 2022-01-05
tags:
  - Architecture
layout: layouts/docs.njk
---

# MCP Client and Server Architecture

## What is the Model Context Protocol?

The [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) enables AI applications to access external tools, data sources, and
prompts. It serves as a bridge between AI models and the outside world, allowing language models to interact with application-specific resources and
functionality.

MCP provides several core capabilities:

- Tools: Functions that can be invoked by LLM applications
- Resources: Data that can be accessed by LLM applications
- Prompts: Templates for LLM interactions
- Notifications: Event-based communication between server and client

In short, MCP gives LLM applications a standardized way to interact with external resources and tools.

## Architecture

The AJ MCP SDK is structured around several core components that work together to implement the MCP specification:

![](/asset/imgs/a-1.jpg)

The architecture follows a client-server model with three transports: standard I/O pipes, the legacy two-endpoint HTTP/SSE transport, and Streamable HTTP. All transports exchange JSON-RPC messages; Streamable HTTP is available for MCP `2025-03-26` and `2025-06-18`.


# Stdio Mode vs SSE Mode Comparison

| Feature | STDIO | Legacy HTTP/SSE | Streamable HTTP |
|---|---|---|---|
| **Deployment** | Local subprocess | Independent server | Independent server |
| **Configuration** | Subprocess command | Separate SSE and POST endpoints | One MCP endpoint; optional GET event stream |
| **Multi-client support** | One process connection | Supported | Supported by session ID |
| **Network requirement** | None | Required | Required |
| **Protocol revisions** | All implemented revisions | Legacy-compatible transport | `2025-03-26` / `2025-06-18` |
