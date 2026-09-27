# AJ MCP Next Client

Java 8, modern MCP `2026-07-28` only. Depends on `aj-mcp-next-core` and the refactored
`com.ajaxjs:aj-net:2.1`; no Spring, OkHttp or legacy SDK dependency.

## HTTP

```java
try(McpClient client=new McpClient(new HttpClientTransport("http://127.0.0.1:8080/mcp"))){
        McpResult discovery=client.discover();
        List<ToolDefinition> tools=client.listTools();
        McpResult result=client.callTool("echo",Collections.singletonMap("text","hello"));
        }
```

Imports: `com.ajaxjs.mcp2026.client.*`, `com.ajaxjs.mcp2026.core.*`, `java.util.*`.
A client owns its transport. Create a separate client for each endpoint/authorization context.
The HTTP constructor accepts fixed custom headers, I/O timeout and concurrency. TLS certificate/hostname checks
remain enabled. Redirects are refused; authorization is never forwarded to a redirected endpoint.
AJ HTTP initializes each connection; its payload-logging `connect()` is deliberately not used.

Every request uses an independent POST. JSON and request-scoped SSE responses are supported, including
comments/multiline data. There are no session IDs, GET streams, resume, handshake or automatic replay.
HTTP standard headers and nested `x-mcp-header` properties are mirrored and safely encoded.
Invalid header-annotated tools are excluded from HTTP `tools/list` results.

`callTool` refreshes tool descriptors first (including all pages) for HTTP.
For low-level `call`, `callAsync`, `exchange` and `callWithInputs` with `tools/call`,
first call `listTools()`, or explicitly register a trusted descriptor with `HttpClientTransport.tool()`.
Descriptors are binding information, not a freshness/authorization cache. Refresh on changes;
HeaderMismatch is surfaced, not automatically retried.

## STDIO

```java
try(McpClient client=new McpClient(StdioClientTransport.launch(
        Arrays.asList("java","-cp",runtimeClasspath,"com.ajaxjs.mcp2026.server.example.EchoServer")))){
        McpResult result=client.callTool("echo",Collections.singletonMap("text","hello"));
        }
```

The child inherits stderr separately. UTF-8 newline frames are serialized; the transport owns child/streams.
Close tries stdin EOF, then bounded termination/forced termination. Supplied custom streams must support
close unblocking concurrent I/O. Configure host logging to stderr, never protocol stdout.

## Calls and bounds

- `call(method, params, timeoutMs)`: synchronous result; interruption cancels the request and restores interruption.
- `callAsync(...)`: result future; cancelling it cancels the actual exchange.
- `request(...)` / `exchange(request, timeoutMs)`: low-level correlated response API.
- `listAll(method, field, maxPages)`: bounded pagination with repeated-cursor detection.
- Timeout zero means 30 seconds; negative values are rejected. All pending requests fail on close/transport failure.
- `McpRemoteException` carries the peer's `RpcError`; synchronous failures are wrapped in `CompletionException`.
- Default HTTP: 15-second connect/read inactivity timeout, 16 I/O workers, 128 queued operations.
  Request deadlines also include queue time. Cancellation disconnects only the affected HTTP exchange.
- Notification callbacks and synchronous future continuations run on receive/completion threads.
  Keep them short; use your own executor/`thenApplyAsync` for blocking work.
- Progress tokens supplied in custom metadata should be unique among active requests.
- Messages/lines/events are bounded to 4 MiB; parser depth limits also apply.

## MRTR and subscriptions

Create metadata explicitly for capabilities you actually implement:

```java
RequestMetadata metadata=RequestMetadata.create(
        Collections.singletonMap("roots",Collections.emptyMap()));
        try(McpClient client=new McpClient(transport,metadata,notification->{})){
        client.listTools(); // Required for low-level HTTP tool calls.
        CompletableFuture<McpResult> result=client.callWithInputs(
        "tools/call",Collections.singletonMap("name","roots"),30_000,8,
        inputRequired->Collections.singletonMap("r",
        Collections.singletonMap("roots",Collections.emptyList())));
        }
```

Import `java.util.concurrent.CompletableFuture`. Input-handler keys must match actual requested input IDs;
this example assumes the server requested `r`. The SDK validates roots/sampling/elicitation results and accepted
form data. It never silently approves a dialog, accesses local files or calls an LLM.
The total deadline includes input-handler time; maxRounds bounds all attempts. New IDs and unchanged opaque
state are used for continuations. Input handlers must cooperate with interruption; Java cannot forcibly stop
arbitrary application code.

`subscribe(filter, lifetimeMs, updates)` returns `Subscription` with `acknowledged()`, `completion()`
and `close()`. Acknowledgment precedes updates; IDs/accepted filters are validated.
Zero lifetime is the finite 30-second default, not infinite. Use a longer explicit lifetime for subscriptions.
Re-listening after completion/disconnect is an application choice; it never replays tool calls.

[中文](README.zh-CN.md)
