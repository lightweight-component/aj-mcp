# AJ MCP Next Server

Java 8; production depends only on core (AJ Util is reused transitively).
Client is test-scope only. Registry/dispatcher, STDIO and JDK HTTP adapters live in this one module.

## Minimal embedded HTTP server

```java
Map<String, Object> schema=new LinkedHashMap<>();
        schema.put("type","object");
        schema.put("properties",Collections.singletonMap("text",Collections.singletonMap("type","string")));
        schema.put("required",Collections.singletonList("text"));
        McpServer server=new McpServer("example","1");
        server.tool(ToolDefinition.create("echo",schema),context->{
        Map<?, ?> arguments=(Map<?, ?>)context.request().getParams().get("arguments");
        return ToolResults.text((String)arguments.get("text"));
        });
        HttpServerTransport http=new HttpServerTransport(server,8080);
        http.start();
// Keep your application running; at application shutdown:
        http.close();
        server.close();
```

Imports: `com.ajaxjs.mcp2026.server.*`, `com.ajaxjs.mcp2026.core.*`, `java.util.*`.
Do not immediately close the transports in real application startup.
For STDIO, see the executable [EchoServer](src/main/java/com/ajaxjs/mcp2026/server/example/EchoServer.java);
`StdioServerTransport.run()` blocks until EOF/close. Only protocol JSON belongs on stdout.

## Registration and protocol flow

- `tool(definition, handler)`: compile schemas once; validate arguments before invocation and structured output
  afterward.
- `resource(descriptor, handler)` / `prompt(descriptor, handler)`: immutable descriptor snapshots, automatic
  lists/routing.
  Required prompt arguments are checked. Read results need `contents`, `ttlMs` and `cacheScope`.
- `method(name, handler)`: customize a known core method, such as resource templates, pagination,
  authorization-aware lists or completion. Custom handlers own their provider-specific validation and authorization.
- Built-in lists are one page, sorted by registry key, TTL 0/private. Unknown cursors are rejected.
- `server/discover` is an ordinary request, not a session handshake; metadata/version is checked on every request.
- Completion is advertised only after registering its handler. Empty resource-template listings are supported;
  URI-template resolution is an optional provider implemented through `method`.
- Tool business failures can return `ToolResults.error`; malformed requests produce protocol errors.
  Unhandled handler exceptions become a generic InternalError; the configurable error hook receives the original cause.
  No payload, credentials or exception details are copied into that wire error.
- `Context` exposes request, authenticated Principal, cancellation, progress and request-level logging.
  Handlers should use interruptible I/O or `checkCancelled()`; state is not stored in a protocol session.
- Registry updates send subscribed list-change notifications. `resourceUpdated(uri)` emits exact-URI updates.
  Only core requested filters are acknowledged; unknown extension filters are not advertised as implemented.

## Lifecycle and hosting

Server defaults: 8 worker threads, 128 queued jobs; 30-second execution deadline, 5-minute subscription lifetime.
Each connection has an in-flight limit of 128. Duplicate active IDs terminate that connection to avoid ambiguous
replies.
Timeout/close interrupts work and suppresses late output. A subscription is acknowledged before updates and completes
at its finite lifetime; STDIO server shutdown cancels active subscriptions. Notifications never receive RPC responses.

The HTTP adapter defaults to `127.0.0.1`, `/mcp`, rejects every supplied Origin unless allowlisted, and requires
POST + JSON with JSON/SSE Accept headers. GET/DELETE return 405. Header/body mismatches return 400/-32020.
Every successful request uses a separate SSE response; heartbeat comments help detect disconnect cancellation.
There is no session affinity or GET event channel. Closing the adapter does not close the reusable server automatically.
The adapter defaults to 32 HTTP workers and a 128-entry queue; each active subscription occupies an HTTP worker.

The extended HTTP constructor accepts address/path/origins and an `Authentication` callback.
It must validate credentials and return a trusted Principal or throw SecurityException (401).
The default loopback example is unauthenticated. Origin filtering is neither authentication nor a CORS policy.
For remote deployment require authentication, TLS termination, body/header/read/write time limits and rate limits.
JDK HttpServer/HttpURLConnection are blocking adapters; this library does not globally change JVM HTTP settings
or promise a hard wall-clock bound for a stalled OS/socket write. Protocol request deadlines are not a substitute
for host/proxy network timeouts. For higher concurrency, adapt `server.connect(sink, principal, true)` to your
framework.

## MRTR state

Handlers return `McpResult.inputRequired(inputs, opaqueState)`; the client owns interaction.
All requested input capabilities and result shapes are checked before emission.
Use `RequestState` with a deployment-managed random 128-bit key:

```java
RequestState states=new RequestState(RequestState.newKey(),"my-service");
// First round:
        String token=states.seal(context.request(),context.principal(),
        Collections.singletonMap("stage","confirm"),60_000);
// Continuation:
        Map<String, Object> state=states.open(context.request(),context.principal());
```

This reuses AJ Util AES-GCM with a fresh nonce. Tokens expire, have bounded size, and bind to audience,
authenticated principal, method and original parameters/metadata (excluding inputResponses/requestState/ID).
Use a stable key across replicas; the example's generated key lasts only for that process.
It does not provide key rotation, one-use replay prevention or business authorization. Persist a nonce/idempotency
record
when one-use effects require it. Validate every input response using `ProtocolSchema` and your business rules;
a valid state token does not make client-provided answers trustworthy. Missing inputs should prompt another
input_required round; ignore unrequested extra inputs.

[中文](README.zh-CN.md)
