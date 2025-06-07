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

In short, MCP provides the rich and diverse context for LLM applications to interact with external resources and tools.

## Architecture

The AJ MCP SDK is structured around several core components that work together to implement the MCP specification:

![](/asset/imgs/a-1.jpg)

The architecture follows a client-server model with flexible transport options. The client interacts with the server using JSON-RPC over either HTTP (
with Server-Sent Events) or standard I/O pipes.


# Stdio Mode vs SSE Mode Comparison

| Feature            | Stdio Mode                  | SSE Mode                     |
|--------------------|-----------------------------|------------------------------|
| **Deployment**     | Local subprocess            | Independent server           |
| **Use Case**       | Local development           | Distributed environments     |
| **Configuration**  | Complex                     | Simple                       |
| **Multi-Client Support** | Not supported             | Supported                    |
| **Network Requirement**  | None                      | Requires network connection  |