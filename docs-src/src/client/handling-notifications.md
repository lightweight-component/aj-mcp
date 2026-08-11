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

JSON-RPC notifications do not contain an `id` and must not receive a response. During initialization, the client sends `notifications/initialized`; both the stdio and SSE server transports suppress output for that notification.

Use `onNotification(method, handler)` to register callbacks for progress, resource-update, logging, and list-change notifications. Resource, resource-template, and prompt caches are invalidated automatically when the corresponding list-change notification arrives.

The client can also answer server-initiated requests with `onServerRequest(...)`. The convenience methods `setRoots(...)` and `setSamplingHandler(...)` configure the standard `roots/list` and `sampling/createMessage` handlers before `initialize()` advertises those capabilities.

When an SSE channel closes or fails, or when a transport is closed, all pending request futures are completed exceptionally. Likewise, an unexpected stdio subprocess exit fails pending requests immediately, including when `requestTimeout` is zero.
