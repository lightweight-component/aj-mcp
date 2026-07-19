---
title: Initializing the Protocol
subTitle: 2024-12-05 by Frank Cheung
description: Initializing the Protocol
date: 2022-01-05
tags:
  - initialization
layout: layouts/docs.njk
---

# Initializing the Protocol

After creating a client, call `initialize()` before sending any other protocol request. It starts the transport, negotiates the protocol version and capabilities, and sends the `notifications/initialized` notification.

The Initialize method:

1. Sends an initialize request to the server
1. Logs the server's initialization result
1. Sends an "initialized" notification

After successful initialization, the client can make other requests to the server.

Call `initialize()` immediately after building the `McpClient`. Initialization uses the configured `requestTimeout`; zero means wait indefinitely.

``` java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();
        
mcpClient.initialize();
```
