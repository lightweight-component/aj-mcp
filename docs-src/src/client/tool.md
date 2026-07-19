---
title: Working with Tools
subTitle: 2024-12-05 by Frank Cheung
description: Working with Tools
date: 2022-01-05
tags:
  - Tools
layout: layouts/docs.njk
---

# Working with Tools

The tool system in AJ MCP provides a structured way to define callable functions that clients can discover and invoke.
Each tool has a name, description, and a JSON Schema that defines its expected input parameters.
The tool system is designed to make it easy for LLMs to understand what tools are available and how to use them.

## Listing Tools

To list available tools:

```java
List<ToolItem> tools = mcpClient.listTools();
assertEquals(7, tools.size());
```

`listTools()` requests the first/default page. It does not automatically follow `nextCursor`. Use `listTools(int pageNo)` to request another page.

``` java
List<ToolItem> tools = mcpClient.listTools(1);
assertEquals(3, tools.size());
```

## Calling Tools

To call a tool:

```java
String toolExecutionResultString = mcpClient.callTool("echoString", "{\"input\": \"hi\"}");
assertEquals("hi", toolExecutionResultString);
```

The current `callTool()` convenience API returns a `String` assembled from text content. It does not expose image content through this method. Tool-level errors are returned as an error message string; transport and timeout failures follow the client exception/timeout behavior.

## Handling Notifications

The current Java client does not expose a public callback-registration API for progress or list-change notifications. See [Handling Notifications](handling-notifications) for the supported behavior.
