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

## Expired Streamable HTTP sessions

When a session-bound POST or GET returns HTTP 404, the transport starts a new initialization handshake without
the old session ID. Pending calls fail and are **not replayed**, including tool calls that may have side effects.
New calls wait for recovery; `transport.getSessionRecovery()` exposes its completion or failure.
Recovery clears the old GET stream and event cursor, then reopens GET if enabled. A failed recovery is not retried
indefinitely. If the server selects a different protocol revision, create a new client explicitly.
Application-owned state, such as resource subscriptions, must be restored by the application after recovery.

After creating a client, call `initialize()` before sending any other protocol request. It starts the transport,
negotiates the protocol version and capabilities, and sends the `notifications/initialized` notification.

The Initialize method:

1. Sends an initialize request to the server
1. Logs the server's initialization result
1. Sends an "initialized" notification

After successful initialization, the client can make other requests to the server.

The server validates the initialize payload before negotiation. `params` and `capabilities` must be JSON objects;
`protocolVersion`, `clientInfo.name`, and `clientInfo.version` must be non-empty strings. Invalid client input is
reported as JSON-RPC `INVALID_PARAMS` instead of an internal error.

Register client capabilities before calling `initialize()`. For example, call `setRoots(...)`,
`setSamplingHandler(...)`, or `setElicitationHandler(...)` first; only registered handlers are advertised. A server-side
call to Roots or Sampling is rejected locally when the target session did not advertise that capability.

Call `initialize()` immediately after building the `McpClient`. Initialization uses the configured `requestTimeout`;
zero means wait indefinitely.

``` java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();
        
mcpClient.initialize();
```
