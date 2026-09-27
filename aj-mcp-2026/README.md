# AJ MCP Next (Java 8)

An independent SDK project targeting MCP `2026-07-28`, reusing AJ Util where possible.
Core is published as `com.ajaxjs:aj-mcp-next-core`, inheriting its version from `ajaxjs-parent`.
The directory is still `aj-mcp-2026`. Core, client and server are implemented as three modules.

| Module                             | Responsibility                                                                      | Dependency boundary                                                                               |
|------------------------------------|-------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| [mcp-core](mcp-core/README.md)     | Messages, codec, method schemas, tool contracts and stateless protocol helpers      | AJ Util, Jackson 2, networknt Schema validator; no HTTP, Servlet, Spring or legacy MCP dependency |
| [mcp-client](mcp-client/README.md) | Calls, deadlines, MRTR, subscriptions, STDIO/HTTP client adapters                   | Core + refactored AJ HTTP; no framework                                                           |
| [mcp-server](mcp-server/README.md) | Registration, dispatch, request lifecycle, protected state, STDIO/JDK HTTP adapters | Core only in production; client is test-scope                                                     |

Production packages use `com.ajaxjs.mcp2026`, allowing coexistence with the legacy SDK.
This project does not inherit the legacy protocol-version enum or initialization lifecycle.
The repository root POM aggregates this project; it can also be built independently:

```sh
mvn -f aj-mcp-2026/pom.xml -DskipTests=false test
mvn -f aj-mcp-2026/pom.xml -DskipTests=false package
mvn -f aj-mcp-2026/pom.xml -DskipTests=false verify
```

Run these commands from the aj-mcp repository root. From this directory use `mvn -DskipTests=false test`.
Compilation targets Java 8 APIs; the build contains no machine-specific JDK paths.

## Local AJ Util dependency

Core uses the refactored `com.ajaxjs:ajaxjs-util:1.3.8`, specifically `com.ajaxjs.util.json.JsonEngine`.
If the same-version artifact in your local Maven repository predates the refactor, first install the source
from the neighboring aj-util checkout:

```sh
mvn -f ../aj-util/aj-util/pom.xml -DskipTests=true -Dmaven.test.skip=true install
mvn -f ../aj-util/aj-http/pom.xml -DskipTests=true install
```

This is dependency preparation only: it skips AJ Util tests and does not validate the entire utility library.
This project explicitly supplies Jackson databind and jsr310 `2.22.1`, matching the current AJ Util POM,
because AJ Util declares them as `provided`. Core neither changes the global `JsonUtil` engine nor
duplicates its general JSON helpers.

## Scope

Implemented: four JSON-RPC message types, string/integer IDs, per-request metadata, standard errors,
capability checks, strict codec, tool contracts, offline JSON Schema validation, official method schemas,
discovery/MRTR/subscription messages and correlated semantic checks. See [core checklist](mcp-core/CORE-SCOPE.md).

Client/server add actual STDIO and request-scoped HTTP/SSE, bounded execution/cancellation, registration,
discovery, resource/prompt/tool routing, completion hooks, MRTR driving, subscription delivery,
HTTP header binding and an authentication hook. State protection reuses AJ Util AES-GCM.

Not provided: legacy fallback, Tasks/extension execution, shared response caching, annotation scanning,
an OAuth issuer or a business replay ledger. Open fields are preserved, not automatically executed.
See [implementation decisions and deployment boundaries](IMPLEMENTATION-NOTES.md).

The adapters compose core without protocol-session state. `verify` also runs the packaged-JAR STDIO example.
References: [protocol](https://modelcontextprotocol.io/specification/2026-07-28/basic/index) and
[official schema](https://modelcontextprotocol.io/specification/2026-07-28/schema).

[中文](README.zh-CN.md)
