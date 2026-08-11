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
- Added tool annotations, progress messages, completion context, resource links, structured tool output, and elicitation.

## aj-mcp-common 1.5 / aj-mcp-client 1.3 / aj-mcp-server 1.2 — 2025-06-10

- Added [pagination support](https://modelcontextprotocol.io/specification/2024-11-05/server/utilities/pagination).

## 1.0 — 2025-06-01

- Initial release.
