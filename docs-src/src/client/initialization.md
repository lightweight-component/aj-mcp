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

After creating a client, you need to initialize it before using it to communicate with the server,
that means, before making any requests, you must initialize the protocol by calling the `initialize()` method.
The `initialize();` method initializes the transport and sets up notification handling.

The Initialize method:

1. Sends an initialize request to the server
1. Stores the server's capabilities
1. Sends an "initialized" notification
1. Marks the client as initialized

After successful initialization, the client can make other requests to the server.

After creating the McpClient, you should call the `initialize();` right away.

``` java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();
        
mcpClient.initialize();
```
