[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-common?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-common)
[![Javadoc](https://img.shields.io/badge/javadoc-1.7-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp-common)
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

# AJ-MCP Common

Common library for AJ-MCP. It contains the shared JSON-RPC messages, MCP protocol models, content types,
protocol-version metadata, transport contracts, exceptions, and JSON utilities used by both the client and server
modules.

[中文](./README.zh-CN.md)

## Features

- Request and response models for initialization, tools, resources, resource templates, prompts, completion, ping,
  pagination, progress, and cancellation.
- Client-side protocol models for roots, sampling, and elicitation.
- Text, image, audio, embedded-resource, and resource-link content models.
- Centralized support for MCP revisions `2024-11-05`, `2025-03-26`, and `2025-06-18` through `ProtocolVersion`.
- Jackson-based `JsonUtils` helpers shared by the SDK.

Applications using `aj-mcp-client` or `aj-mcp-server` receive this module transitively. Add it directly when
implementing a custom transport, constructing protocol messages yourself, or reusing only the protocol model.

## Install

Runs on Java8+. Maven:

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-common</artifactId>
    <version>1.7</version>
</dependency>
```

## Basic usage

Serialize and parse protocol objects with the SDK's configured Jackson mapper:

```java
CallToolRequest request = new CallToolRequest(
        "weather", Collections.<String, Object>singletonMap("city", "Guangzhou"));
request.setId(1L);

String json = JsonUtils.toJson(request);
JsonNode node = JsonUtils.json2Node(json);
```

Inspect or select supported protocol revisions without comparing raw strings throughout your code:

```java
List<String> supported = ProtocolVersion.supportedVersions();
boolean structuredOutput = ProtocolVersion.V_2025_06_18.supportsStructuredToolOutput();
```

`JsonUtils.OBJECT_MAPPER` rejects duplicate JSON object keys and ignores unknown fields when deserializing, matching the
SDK's compatibility behavior.

## Used by

- [`aj-mcp-client`](../aj-mcp-client): client API and transports.
- [`aj-mcp-server`](../aj-mcp-server): feature registration, dispatch, and server transports.
- [User Manual](https://mcp.ajaxjs.com/)
