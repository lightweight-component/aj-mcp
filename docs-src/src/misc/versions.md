---
title: Release History
subTitle: 2024-12-05 by Frank Cheung
description: AJ MCP SDK release history
date: 2022-01-05
tags:
  - releases
layout: layouts/docs.njk
---

# Release History

## Unreleased maintenance changes

- Fixed HTTP response leaks and pending-request cleanup after SSE or stdio failures.
- Completed stdio and server-side SSE shutdown lifecycles.
- Corrected notification response behavior and request timeout consistency.
- Isolated feature stores per server and made package scanning tolerate unloadable optional classes.
- Added per-page caches for prompt, resource, and resource-template lists.
- Added string JSON-RPC IDs, strict initialization lifecycle checks, and truthful dynamic capabilities.
- Completed resource templates, completion, resource subscriptions, cancellation, logging, progress, Roots, Sampling, and generic notification callbacks.
- Added opaque-cursor page APIs while retaining the page-number APIs for compatibility.
- Added negotiation for MCP `2024-11-05`, `2025-03-26`, and `2025-06-18` in one SDK; server version lists are preference ordered.
- Added client and server Streamable HTTP adapters. JSON-RPC batching, removed again in `2025-06-18`, is intentionally unsupported.
- Added tool annotations, progress messages, completion-context request models, resource links, structured tool output, and elicitation. Passing completion context into server provider methods remains planned work.
- Ensured parameterless tools always expose a non-null object `inputSchema` and made every tool parameter require `@ToolArg` during scanning.
- Scoped tool cancellation by session and request ID; session shutdown now interrupts only its own running tools.
- Added strict initialize field validation and capability checks before server-initiated Roots or Sampling requests.
- Added embedded-resource `blob` representation and validated Tool, Prompt, and Resource return collections before serialization.
- Converted client-side server-request handler failures into JSON-RPC `INTERNAL_ERROR` without terminating receive loops.

Streamable HTTP currently supports ordinary JSON POST responses and the optional GET event stream. POST `text/event-stream` responses are buffered rather than dispatched incrementally. GET-stream readiness is asynchronous, and reconnect/resumption plus full idle-session expiry remain planned work.

## aj-mcp-common 1.5 / aj-mcp-client 1.3 / aj-mcp-server 1.2 — 2025-06-10

- Added [pagination support](https://modelcontextprotocol.io/specification/2024-11-05/server/utilities/pagination).

## 1.0 — 2025-06-01

- Initial release.
