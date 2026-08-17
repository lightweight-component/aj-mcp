---
name: aj-mcp-java8
description: Develop, review, debug, test, and document the AJ-MCP Java 8 SDK in this repository. Use for changes involving aj-mcp-common, aj-mcp-client, aj-mcp-server, JSON-RPC/MCP protocol models, protocol-version negotiation, STDIO, legacy HTTP/SSE, Streamable HTTP, annotation-based tools/resources/prompts, lifecycle or concurrency bugs, Maven tests, samples, and the bilingual docs-src documentation.
---

# AJ-MCP Java 8

Maintain this repository as a small MCP SDK for legacy Java 8 systems. Preserve protocol correctness, transport lifecycle safety, and Java 8 compatibility while keeping changes focused.

## Orient in the repository

- Treat `aj-mcp-common` as the shared JSON-RPC/MCP model and utility layer.
- Treat `aj-mcp-client` as the client API, feature-specific clients, caches, timeout behavior, and client transports.
- Treat `aj-mcp-server` as request dispatch, initialization, feature registration/invocation, JSON-RPC errors, and server transports.
- Treat `samples` as executable usage examples, not the primary implementation.
- Treat `docs-src/src/index.md` and `docs-src/src/cn.md` as the English and Chinese documentation pair.
- Read `to-fix.md` before audits or broad fixes. Confirm each entry against current code because resolved items may remain as historical notes.

Before editing, inspect `git status` and preserve unrelated work. Use `rg` and `rg --files` to trace models, dispatch paths, transport behavior, and tests.

## Respect compatibility boundaries

- Keep production code compatible with Java 8. Do not introduce records, `var`, newer collection factories, newer language syntax, or post-Java-8 JDK APIs.
- Keep the three supported protocol revisions centralized in `ProtocolVersion`: `2024-11-05`, `2025-03-26`, and `2025-06-18`.
- Negotiate a supported version during initialization and gate revision-specific behavior through `ProtocolVersion`; do not scatter raw version-string comparisons.
- Keep JSON-RPC batch requests unsupported unless the user explicitly changes project scope.
- Do not silently add dependencies or raise dependency/JDK baselines.

## Follow protocol invariants

- Return no JSON-RPC response for notifications, including failing or invalid notifications. Transport writers must ignore a `null` response.
- Return structured JSON-RPC errors for invalid methods, invalid parameters, and known business failures. Preserve reflection causes and never print stack traces from library code.
- Always expose a non-null object `inputSchema` for every tool. A parameterless tool must at least produce `{"type":"object","properties":{}}`.
- Allow optional annotated arguments to be omitted. Reject missing required arguments, unknown features, invalid numeric conversions, and unsupported return values explicitly.
- Keep request IDs and cancellation state scoped to the connection or session where required; do not use process-global request identity.
- Complete every pending future exceptionally when a transport fails, closes, or its child process exits. A zero configured timeout must not create an accidental infinite wait.
- Restore the thread interrupt flag whenever catching `InterruptedException`, then propagate or translate the interruption.
- Close HTTP response bodies, EventSource instances, executors, streams, worker threads, sessions, and child processes through idempotent lifecycle methods.
- Serialize concurrent writes per STDIO stream or SSE session so JSON lines and SSE frames cannot interleave.
- Check `PrintWriter.checkError()` after network writes because `PrintWriter` suppresses underlying I/O exceptions.
- Load scanned classes without initialization and isolate a single unloadable optional class from the rest of package scanning.
- Keep feature stores instance-scoped. Express an empty store as an empty collection or a clear protocol error, never as `NullPointerException`.

## Implement a change

1. Trace the complete request path across protocol model, client/server API, dispatcher, and transport before selecting the edit point.
2. Check negotiated-version and capability conditions for both directions of server/client requests.
3. Make the smallest compatible change and add short English comments only where lifecycle, concurrency, or protocol reasoning is non-obvious.
4. Add a regression test in the owning module. Prefer deterministic latches, barriers, bounded waits, and fake transports over timing sleeps for concurrency tests.
5. Test success, failure, close, and notification behavior when the change crosses a transport boundary.
6. Update both documentation languages and affected samples when public API, supported behavior, configuration, or lifecycle usage changes.

## Validate proportionally

Run a targeted module test first:

```bash
mvn -pl aj-mcp-client -am -Dtest=TestClassName -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl aj-mcp-server -am -Dtest=TestClassName -Dsurefire.failIfNoSpecifiedTests=false test
```

Then run the affected module suite and, for cross-module changes, the full reactor:

```bash
mvn -pl aj-mcp-client -am test
mvn -pl aj-mcp-server -am test
mvn test
```

Use an installed JDK 8 or JDK 17 when the current machine JDK is too new for the repository's Maven plugins or annotation processors. Report the selected JDK and distinguish environment/toolchain failures from test failures.

Build the documentation after editing it:

```bash
cd docs-src
npx @11ty/eleventy
```

Do not treat generated `docs-src/dist` output as the source of truth. Verify whether the project expects generated files to be committed before changing them.

## Review before handoff

- Check public API compatibility and Java 8 compilation.
- Check notification suppression and JSON-RPC error codes.
- Check timeouts, interruption, pending-request cleanup, and idempotent close behavior.
- Check session isolation and concurrent writer safety.
- Check annotation scanning, optional arguments, empty feature stores, and non-null schemas.
- Check all supported protocol revisions and capability gates.
- Check English/Chinese documentation parity when behavior changed.
- State exactly which tests ran and call out any validation blocked by the local toolchain.
