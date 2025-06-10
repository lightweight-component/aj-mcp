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

This method will fetch all tools across multiple pages without handling pagination.
If you need more control over pagination, you can use the overloaded `listTools(int pageNo)` method instead.

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

The `callTool()` method returns a CallToolResult containing the tool's response, which may include text, images, or other content types.

<!--
For tools that send progress notifications, you can provide a ProgressToken in the request metadata. You'll need to register a notification handler to
receive these updates.

## Handling Notifications

The client can receive notifications from the server using the OnNotification method:

client.OnNotification(func(notification mcp.JSONRPCNotification) {
// Handle notification
if notification.Method == "notifications/progress" {
// Handle progress notification
}
})

You can register multiple notification handlers, which will be called in the order they were registered.

For more detailed information on notification handling, see Handling Notifications.

## Error Handling

All client methods that communicate with the server can return errors. These errors can be:

    Transport errors: Errors that occur in the transport layer
    Protocol errors: Errors returned by the server
    Parsing errors: Errors that occur when parsing responses

It's important to check for errors after each client operation:

result, err := client.SomeOperation(ctx, request)
if err != nil {
// Handle error
return err
}
// Process result

-->