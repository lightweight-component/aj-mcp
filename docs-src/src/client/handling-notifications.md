---
title: Handling Notifications
subTitle: 2024-12-05 by Frank Cheung
description: Notification behavior in the AJ MCP client
date: 2022-01-05
tags:
  - notifications
layout: layouts/docs.njk
---

# Handling Notifications

JSON-RPC notifications do not contain an `id` and must not receive a response. During initialization, the client sends
`notifications/initialized`; the STDIO, legacy HTTP/SSE, and Streamable HTTP server transports suppress output for that
notification.

Use `onNotification(method, handler)` to register callbacks for progress, resource-update, logging, and list-change
notifications. Resource, resource-template, and prompt caches are invalidated automatically when the corresponding
list-change notification arrives.

The client can also answer server-initiated requests with `onServerRequest(...)`. The convenience methods
`setRoots(...)`, `setSamplingHandler(...)`, and `setElicitationHandler(...)` configure the standard `roots/list`,
`sampling/createMessage`, and `elicitation/create` handlers before `initialize()` advertises those capabilities.
Elicitation requires MCP `2025-06-18`.

If a server-request handler throws a runtime exception, the client catches it at the common transport boundary and sends
JSON-RPC `INTERNAL_ERROR` (`-32603`) with the original request ID. The exception is logged but does not terminate the
STDIO, legacy SSE, or Streamable HTTP receive loop. Returning `null` means that no handler result is available and
produces `METHOD_NOT_FOUND`.

When a legacy SSE channel closes or fails, or when a transport is closed, all pending request futures are completed
exceptionally. Likewise, an unexpected STDIO subprocess exit fails pending requests immediately, including when
`requestTimeout` is zero. Streamable HTTP currently treats a GET event-stream failure as a transport failure too; it has
no reconnect/resumption policy.

Cancellation is scoped by both session and JSON-RPC request ID on the server. Two clients may safely use the same
request ID; cancelling one session does not interrupt the other. Closing a session interrupts only that session's
running tools.
