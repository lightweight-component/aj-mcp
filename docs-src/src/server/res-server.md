---
title: MCP Server SDK Resources Development
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK Resources Development
date: 2022-01-05
tags:
  - Resources Development
layout: layouts/docs.njk
---

# MCP Server SDK Feature Management

The AJ-MCP annotation system provides a declarative approach to exposing Java methods as MCP (Model Context Protocol) tools, resources, and prompts.
This system uses Java annotations to automatically discover and register functionality, eliminating the need for manual configuration or registration
code.

## Resources Development

Resources are exposed through the `@Resource` annotation on methods within `@McpService` classes. The annotation system automatically discovers and
registers resources during server initialization.

The `@Resource` annotation exposes methods as MCP resources with the following properties:

| Property    | Required | Description                             |
|-------------|----------|-----------------------------------------|
| uri         | Yes      | Unique identifier for the resource      |
| description | No       | Human-readable description              |
| mimeType    | No       | MIME type of the resource content       |
| value       | No       | Human-readable name (defaults to empty) |

### Resource Content Types

#### Text Resources

Text resources return `ResourceContentText` objects containing plain text or structured text data.

```java
@Resource(uri = "file:///text", description = "A nice piece of text", mimeType = "text/plain")
public ResourceContentText text(){
        ResourceContentText content=new ResourceContentText();
        content.setUri("file:///text");
        content.setMimeType("text/plain");
        content.setText("text888");
        return content;
        }
```

#### Binary Resources

Binary resources return `ResourceContentBinary` objects containing Base64-encoded binary data.

```java
@Resource(uri = "file:///blob", description = "A nice pic", mimeType = "image/jpg")
public ResourceContentBinary blob(){
        String base64EncodedImage=ServerUtils.encodeImageToBase64("bird-eye.jpg");

        ResourceContentBinary content=new ResourceContentBinary();
        content.setUri("file:///blob");
        content.setMimeType("image/jpg");
        content.setBlob(base64EncodedImage);

        return content;
        }
```

### Resource Templates

The `@ResourceTemplate` annotation supports dynamic resources with parameterized URIs following RFC 6570 URI template syntax.

| Property    | Required | Description                   |
|-------------|----------|-------------------------------|
| uriTemplate | Yes      | RFC 6570 Level 1 URI template |
| name        | No       | Human-readable name           |
| description | No       | Template description          |
| mimeType    | No       | Content MIME type             |

Property Required Description
uriTemplate Yes RFC 6570 Level 1 URI template
name No Human-readable name
description No Template description
mimeType No Content MIME type

Template methods accept parameters that correspond to template variables in the URI pattern.
