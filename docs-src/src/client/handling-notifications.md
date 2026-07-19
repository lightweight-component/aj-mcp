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

The current client recognizes `notifications/message` and writes its parameters to the application log. A public callback-registration API for progress, resource-change, tool-list-change, and prompt-list-change notifications is not implemented yet. Do not assume that notification-driven cache invalidation is available.

When an SSE channel closes or fails, or when a transport is closed, all pending request futures are completed exceptionally. Likewise, an unexpected stdio subprocess exit fails pending requests immediately, including when `requestTimeout` is zero.
