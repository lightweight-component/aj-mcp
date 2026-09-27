---
title: AJ MCP Next
description: Java 8 SDK for modern per-request MCP
layout: layouts/docs.njk
---

# AJ MCP Next

The separate aj-mcp-2026 project targets only MCP 2026-07-28. Maven artifacts are
com.ajaxjs:aj-mcp-next-core, aj-mcp-next-client and aj-mcp-next-server; their version follows ajaxjs-parent.
It does not inherit the legacy SDK's initialize handshake or HTTP sessions.

Core supplies messages, strict JSON/schema validation and protocol helpers. Client supplies bounded calls,
MRTR input handlers, subscriptions and STDIO/HTTP. Server supplies registration/dispatch, lifecycle,
AJ Util AES-GCM state protection and STDIO/JDK HTTP. AJ Util JSON and AJ HTTP connection initialization are reused.

Default calls expire after 30 seconds. HTTP defaults to loopback, rejects unapproved Origin, and uses separate
POST response streams. Remote deployment requires authentication, TLS and host network limits.
No automatic tool replay, legacy fallback, shared authorization cache, OAuth issuer or Tasks extension is included.

Repository documentation contains executable examples and complete limitations:
[project](https://github.com/lightweight-component/aj-mcp/tree/main/aj-mcp-2026),
[client](https://github.com/lightweight-component/aj-mcp/tree/main/aj-mcp-2026/mcp-client),
[server](https://github.com/lightweight-component/aj-mcp/tree/main/aj-mcp-2026/mcp-server).
These source changes must be committed/published before they appear in the hosted repository.

From the repository root run `mvn -f aj-mcp-2026/pom.xml -DskipTests=false clean verify`.
verify includes real HTTP, subprocess STDIO and the packaged-JAR example. It is not official conformance certification.
