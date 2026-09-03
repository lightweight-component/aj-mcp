---
title: MCP Server SDK  Tools Development
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK  Tools Development
date: 2022-01-05
tags:
  - Tools Development
layout: layouts/docs.njk
---

# Tools Development

Tools are created by annotating methods with `@Tool` in classes marked with `@McpService`. The framework automatically
discovers these methods and
exposes them as callable tools.

```java

@McpService
public class MyTools {
    @Tool(description = "Echoes a string")
    public String echoString(@ToolArg(description = "The string to be echoed") String input) {
        return input;
    }

    @Tool("customName", description = "Tool with custom name")
    public String myMethod() {
        return "result";
    }
}
```

## Parameter Handling and Validation

Tool parameters are annotated with `@ToolArg` to provide metadata for validation and schema generation. Each parameter
can specify a name, description, and whether it is required.

Every Java parameter of a `@Tool` method must have `@ToolArg`. Package scanning fails fast when an annotation is missing
and reports the class, method, and parameter index. Parameterless tools are valid; their `inputSchema` is always an
object schema with empty `properties`, never JSON `null`.

### Parameter Annotation

| Attribute   | Description                                          | Default      |
|-------------|------------------------------------------------------|--------------|
| value       | Parameter name (uses method parameter name if empty) | Empty string |
| description | Parameter description for clients                    | Empty string |
| required    | Whether parameter is mandatory                       | true         |

## Type Mapping

The framework automatically maps Java types to JSON Schema types:

| Java Type                        | JSON Schema Type |
|----------------------------------|------------------|
| String, Character                | "string"         |
| int, long, float, double, Number | "number"         |
| boolean, Boolean                 | "boolean"        |
| Other types                      | "object"         |

Parameters declared with `required=false` may be omitted. Missing reference-type parameters are passed as `null`;
primitive parameters must still be supplied because Java primitives cannot represent absence.

## Example Tool Implementations

### Basic String Tool

```java
@Tool(description = "Echoes a string")
public String echoString(@ToolArg(description = "The string to be echoed") String input) {
    return input;
}
```

### Parameterless Tool

```java
@Tool(description = "Takes 5 seconds to complete")
public String longOperation() throws Exception {
    TimeUnit.SECONDS.sleep(5);
    return "ok";
}
```

### Content Return Tool

```java
@Tool(description = "A nice pic")
public ContentImage image() {
    String base64EncodedImage = ServerUtils.encodeImageToBase64("bird-eye.jpg");
    ContentImage content = new ContentImage();
    content.setMimeType("image/jpg");
    content.setData(base64EncodedImage);
    return content;
}
```

### Multi-Content Tool

```java
@Tool(description = "List ALL")
public List<Content> getAll() {
    List<Content> list = new ArrayList<>();
    list.add(new ContentImage(/* ... */));
    list.add(new ContentText("Hello World"));
    return list;
}
```

## Return-value Contract

A tool may return a `String`, one `Content`, `List<Content>`, or `StructuredToolResult` for MCP 2025-06-18. Every list
element is validated before serialization. A `null` return, null content list, or list containing another type becomes a
tool result with `isError=true` instead of causing a `NullPointerException` or emitting invalid protocol JSON.

Prompt methods must return `PromptMessage` or `List<PromptMessage>`. Resource methods must return `ResourceContent` or
`List<ResourceContent>`. Invalid or null Prompt/Resource results are reported as JSON-RPC `INTERNAL_ERROR`.
