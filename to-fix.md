# aj-mcp remaining issues

> Review date: 2026-08-11  
> Scope: Java sources in `aj-mcp-common`, `aj-mcp-client`, and `aj-mcp-server`.  
> Method: static source review plus inspection of the existing tests. No production source was changed during this
> review.

## Priority definition

- **P0**: breaks a core protocol path, can affect another session, or makes an advertised feature unusable.
- **P1**: definite bug/resource leak/protocol violation that needs a realistic but narrower trigger.
- **P2**: robustness, API consistency, validation, or maintainability issue with a practical failure mode.

## Summary

| Priority  |  Count | Main themes                                                                                                   |
|-----------|-------:|---------------------------------------------------------------------------------------------------------------|
| P0        |      1 | Streamable HTTP POST streaming                                                                                |
| P1        |     11 | HTTP/SSE lifecycle, session cleanup, notification errors, reflection boundaries, capability/logging semantics |
| P2        |      3 | JSON-RPC validation, schema expressiveness, cache/API safety, resource cleanup                                |
| **Total** | **15** |                                                                                                               |

## P0

### 1. Streamable HTTP POST responses are not actually streamed on either side

**Evidence**

- Client: `aj-mcp-client/src/main/java/com/ajaxjs/mcp/client/transport/StreamableHttpTransport.java:143-167`
- Server: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/ServerStreamableHttp.java:97-106`

The client calls `response.body().string()` before checking/processing `text/event-stream`. That method waits for EOF
and buffers the entire response, so a long-lived POST SSE response never delivers incremental messages and may never
complete. The server always returns either one JSON body or 202 and has no representation for a POST-owned SSE response.

**Impact**

Progress, sampling, elicitation, or other server-originated messages during a request cannot use the Streamable HTTP
POST response channel. With servers that keep that SSE response open, the client request can hang until its timeout. The
transport advertises support for 2025-03-26/2025-06-18 but a central transport behavior is missing.

**Suggested direction**

Introduce a streaming response abstraction on the server and parse the response body incrementally on the client (or use
an EventSource-compatible reader for POST). Correlate the final JSON-RPC response while continuing to dispatch
intermediate requests/notifications. Add an integration test whose POST response sends two SSE events without closing
between them.

### Resolved: cancellation state was keyed only by request ID

**Evidence**

- State: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/McpServer.java:48-49`
- Cancellation lookup: `McpServer.java:247-259`
- Tool registration/cleanup: `McpServer.java:582-600`

JSON-RPC IDs are unique only within one connection/session. `runningRequests` and `cancelledRequests` use only
`String.valueOf(requestId)`. Two HTTP/SSE clients commonly both issue request `1`; cancellation from one client can
interrupt the other client's tool thread. A cancellation received before registration can also affect a later unrelated
reuse of the same ID.

**Impact**

One client can cancel another client's work. This is both a correctness and tenant-isolation problem.

**Resolution (2026-08-11)**

Running requests now use a composite `(sessionId, requestId)` key. Unknown cancellation IDs are not retained, duplicate
running IDs are rejected only within the same session, and closing a session interrupts only that session's requests.
Tests cover two sessions using the same request ID, unknown cancellation followed by ID reuse, and session-scoped
cleanup. This item is no longer included in the remaining-issue count.

## P1

### 3. Optional GET SSE failure incorrectly fails unrelated POST requests

**Evidence**: `aj-mcp-client/src/main/java/com/ajaxjs/mcp/client/transport/StreamableHttpTransport.java:238-251`

`openGetStream()` calls `failPendingRequests()` on any GET EventSource failure. In Streamable HTTP, ordinary requests
have their own POST response channels and the optional GET may be rejected (for example 405) without invalidating those
POST calls.

**Suggested direction**: track GET-stream health separately. Only fail all requests when the whole session is known to
be invalid; otherwise report/retry the optional stream without touching POST futures.

### 4. GET SSE has no reconnect or event resumption

**Evidence**: `StreamableHttpTransport.java:238-252`

There is no `onClosed` handling, retry policy, last-event-id tracking, or resumption. After a transient disconnect,
server notifications and server-initiated requests silently stop for the rest of the client lifetime.

**Suggested direction**: model the stream state explicitly, reconnect with bounded backoff, propagate the last SSE event
ID where supported, and make retry/terminal failure observable.

### 5. Streamable HTTP sessions are not terminated by the client and can leak on the server

**Evidence**

- Client close: `StreamableHttpTransport.java:262-275`
- Server close: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/ServerStreamableHttp.java:229-237`

The client never sends the transport's HTTP DELETE before locally shutting down. On the server, `close()` removes only
session IDs present in `streams`; a successfully initialized session that never opened GET is not in that map and
remains in `McpServer` state. There is also no idle expiration.

**Suggested direction**: send DELETE best-effort before client shutdown, keep an explicit server session registry
independent of GET streams, and add idle/session-expiry cleanup.

### 6. Streamable HTTP initialization returns before the GET channel has succeeded

**Evidence**: `StreamableHttpTransport.java:72-81,238-252`

`initialize()` starts `openGetStream()` asynchronously and immediately completes. A client can therefore advertise
roots/sampling/elicitation and appear initialized even when the only channel capable of receiving server requests has
already failed or is rejected.

**Suggested direction**: when server-request capabilities require GET, wait for `onOpen` (with the normal initialization
timeout) or fail initialization. If GET is intentionally optional, expose its readiness separately.

### 7. Server Streamable HTTP GET streams have no disconnect/heartbeat lifecycle

**Evidence**: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/ServerStreamableHttp.java:119-143,257-281`

The adapter stores a caller-owned `PrintWriter`, returns immediately, and only discovers a dead connection on a later
write. It has no heartbeat, disconnect callback, idle timeout, or asynchronous response ownership contract. A quiet
disconnected client can remain registered indefinitely.

**Suggested direction**: define an async stream handle with `onClose`, heartbeat scheduling, last-activity time, and
idempotent removal. Document which layer owns and closes the servlet response/writer.

### 8. Streamable HTTP sends JSON-RPC errors for notifications

**Evidence**: `ServerStreamableHttp.java:97-110`

If `processMessage()` throws for an envelope without an ID, both catch blocks return a JSON error body. JSON-RPC
notifications must not receive a response, including when their parameters or method are invalid.

**Suggested direction**: determine `expectsResponse` before processing, as `ServerStdio` already does, and return 202/no
body for notification-shaped messages while logging the failure locally.

### 9. Server-to-client requests can wait forever

**Evidence**: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/McpServer.java:134-161`

`timeout == null` and `Duration.ZERO` both use unbounded `future.get()`. A missing client response or an undetected
broken GET/SSE channel can permanently consume the caller thread. This is especially dangerous when invoked from
request-processing executors.

**Suggested direction**: require a finite default server-request timeout; reserve unlimited waiting for an explicitly
named opt-in. Ensure transport disconnection completes all session futures exceptionally.

### Resolved: roots and sampling requests ignored negotiated client capabilities

**Evidence**: `McpServer.java:104-117` compared with elicitation checks at `McpServer.java:122-131`

`elicit()` verifies both protocol revision and advertised capability, but `listRoots()` and `createMessage()` send
requests without checking `roots` or `sampling`. A server can invoke methods the client did not advertise.

**Resolution (2026-08-11)**: `listRoots()` and `createMessage()` now require the corresponding capability in the target
session before writing to the transport. Tests cover both advertised and missing capabilities.

### 11. Logging level is global and is not used as a filter

**Evidence**

- Global field/update: `McpServer.java:47,265-275`
- Broadcast without threshold check: `McpServer.java:315-324`

One session's `logging/setLevel` changes the default level for every client. `publishLog()` then broadcasts every
message and does not compare the message severity with the configured threshold, so `setLevel` does not perform its
intended filtering.

**Suggested direction**: store the level per session, filter by severity, send only to that session, and remove the
setting with the rest of session state.

### Resolved: tool methods with unannotated Java parameters were registered but failed at invocation

**Evidence**

- Registration only records `@ToolArg`:
  `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/feature/FeatureMgr.java:186-207`
- Invocation sizes arguments from that reduced list:
  `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/McpServer.java:554-589`

A method may have Java parameters that lack `@ToolArg`. They are omitted from schema and `paramsOrder`, yet reflection
still invokes the original method, producing an argument-count mismatch. That `IllegalArgumentException` is not
converted to a tool error by the local catch clauses.

**Resolution (2026-08-11)**: feature scanning now rejects every tool parameter without `@ToolArg` and reports the
declaring class, method, and parameter index. A dedicated invalid-service fixture verifies startup-time failure.

### 13. Tool numeric conversion silently truncates or overflows

**Evidence**

- All numeric types advertised as `number`: `FeatureMgr.java:253-276`
- Narrowing conversions: `McpServer.java:650-688`

Examples include JSON `1.9` becoming Java `int` value `1`, or an out-of-range number wrapping through `byteValue()`/
`intValue()`. This executes business logic with values different from those sent by the client.

**Suggested direction**: emit JSON Schema `integer` for integral types and validate fraction/range before conversion.
Convert failures to `INVALID_PARAMS`, not a generic internal error.

### Resolved: feature return conversion accepted invalid values and crashed on null

**Evidence**: `McpServer.java:606-627`

`null` reaches `returnedValue.toString()` and throws. Any `List` is unchecked-cast to `List<Content>`, so a
`List<String>` can be serialized into a response that violates the declared content schema. Similar unchecked
return-list handling exists for prompts (`McpServerPrompt.java:150-159`) and resource templates (
`McpServerResource.java:167-172`).

**Resolution (2026-08-11)**: tool null/invalid-list returns now become `isError=true` tool results. Prompt and resource
null/invalid-list returns become explicit `INTERNAL_ERROR` responses. Every collection element is validated before
serialization, with regression tests for all three feature types.

### 15. Cancellation success can be returned after a cancellation request

**Evidence**: `McpServer.java:582-604`

The server interrupts the worker but relies on tool code to honor interruption. If the method clears/ignores the
interrupt and returns normally, the server sends a successful result. The `finally` block clears the interrupt without
checking cancellation outcome.

**Suggested direction**: keep cancellation state through result construction and suppress/convert a result when
cancellation won the race. Document that cancellation is cooperative, while ensuring the protocol result is internally
consistent.

### 16. Completion context from 2025-06-18 is parsed but ignored

**Evidence**: `McpServer.java:394-412`; registration requires exactly one `String` at `FeatureMgr.java:140-153`

`CompleteRequest.Params.context` is accepted by the DTO/client, but the completion provider receives only the current
argument value. Providers cannot use previously resolved arguments, so context-dependent completion behaves incorrectly
while appearing supported.

**Suggested direction**: add a backward-compatible provider signature/context object and dispatch based on the
registered method signature. Cover both legacy single-value and context-aware providers.

### Resolved: legacy `HttpMcpTransport(String)` constructed an unusable instance

**Evidence**: `aj-mcp-client/src/main/java/com/ajaxjs/mcp/client/transport/HttpMcpTransport.java:68-84`

The one-argument public constructor sets only `sseUrl`; the `OkHttpClient` is initialized only by the builder
constructor. Starting the one-argument instance dereferences a null client.

**Resolution (2026-08-11)**: the convenience constructor now delegates to the fully initializing constructor. A
direct-constructor start/failure test verifies that the HTTP client exists and the EventSource is cleaned up.

## P2

### Resolved: JSON-RPC validation accepted non-string `jsonrpc` and `method` values

**Evidence**: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/McpServerInitialize.java:162-170,193-203`

Jackson's `asText()` turns numeric nodes into text, so numeric `2.0` can pass as the version and a numeric method
becomes a method-name string. Required types should be checked explicitly. Missing method is also reported as
`METHOD_NOT_FOUND` even when the envelope itself is structurally invalid.

**Resolution (2026-08-11)**: `jsonrpc` and `method` must now be textual, and missing/empty/non-textual methods produce
`INVALID_REQUEST`. Validation of the optional `params` shape remains part of the general initialize/request-validation
item below.

### Resolved: invalid initialize parameters became internal/runtime errors

**Evidence**: `McpServerInitialize.java:60-70,89`; session capability storage at `McpServer.java:78-83`

Deserialization failure is wrapped in a plain `RuntimeException`; missing `params`, `protocolVersion`, or `capabilities`
can lead to null dereferences (including `ConcurrentHashMap.put` with a null capability). Clients receive internal error
instead of `INVALID_PARAMS`.

**Resolution (2026-08-11)**: initialization now validates object-shaped params and capabilities, a non-empty textual
protocol version, and non-empty textual client name/version. Deserialization failures are translated to
`INVALID_PARAMS`. Tests cover missing and incorrectly typed fields.

### 20. JSON Schema models discard valid schema keywords

**Evidence**

- `aj-mcp-common/src/main/java/com/ajaxjs/mcp/protocol/tools/JsonSchema.java:13-22`
- `JsonSchemaProperty.java:10-18`
- Output-schema parsing: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/feature/FeatureMgr.java:222-226`

The models support only `type`, `properties`, `required`, `additionalProperties`, and property descriptions. Because
unknown JSON fields are globally ignored, valid keywords such as `items`, `enum`, `oneOf`, numeric/string constraints,
and nested schemas disappear silently from annotation-provided output schemas and client-parsed schemas.

**Suggested direction**: represent schemas with `JsonNode`/`Map<String,Object>`, or add `@JsonAnyGetter/@JsonAnySetter`
so unknown valid keywords round-trip unchanged.

### Resolved: embedded binary resources could not be represented

**Evidence**: `aj-mcp-common/src/main/java/com/ajaxjs/mcp/protocol/common/ContentEmbeddedResource.java:20-36`

The comment promises either text or base64 blob data, but the model has only `text` and no `blob` field. Binary embedded
resources therefore cannot be emitted or deserialized correctly.

**Resolution (2026-08-11)**: the embedded resource model now exposes an optional `blob` property for base64 data. A
serialization test covers the wire field. Enforcing mutual exclusion with `text` remains a possible future validation
enhancement, not a representation blocker.

### Resolved: client server-request handler exceptions could terminate message processing

**Evidence**

- Handler application: `aj-mcp-client/src/main/java/com/ajaxjs/mcp/client/McpClientBase.java:200-204`
- Transport dispatch: `aj-mcp-client/src/main/java/com/ajaxjs/mcp/client/transport/McpTransport.java:166-180`

User handlers are called without a protective boundary. A runtime exception can escape the SSE callback or STDIO reader
path; no JSON-RPC error response is sent to the server, and the receiving channel may terminate.

**Resolution (2026-08-11)**: the common client transport boundary now catches runtime handler failures, logs the cause,
and sends JSON-RPC `INTERNAL_ERROR` with the original request ID. The exception no longer escapes into STDIO/SSE receive
loops.

### 23. Client cache and convenience API behavior is unsafe/inconsistent

**Evidence**

- Mutable cached lists returned directly:
  `aj-mcp-client/src/main/java/com/ajaxjs/mcp/client/McpClientPrompt.java:68-87`,
  `McpClientResource.java:160-178,189-207`
- Legacy tool listing skips protocol-error checking:
  `aj-mcp-client/src/main/java/com/ajaxjs/mcp/client/McpClient.java:75-97`
- String tool helper rejects non-text content: `McpClient.java:215-260`

Callers can mutate cached prompt/resource lists and corrupt later results. `listTools(int)` dereferences `result.tools`
without first calling `McpException.checkForErrors()`, turning a valid JSON-RPC error into a null/cast failure. The
string convenience method also throws for image/audio/resource content even though the complete-result API supports
them.

**Suggested direction**: return immutable snapshots, route old page-number APIs through the checked cursor-page
implementation, and either document the text-only helper strictly or define deterministic rendering for non-text
content.

**Partial resolution (2026-08-11)**: `listTools(int)` now calls `McpException.checkForErrors()` and has a regression
test. Mutable cache exposure and non-text convenience-result behavior remain open.

### Resolved: resource subscription maps retained empty URI entries

**Evidence**: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/McpServer.java:64-66,341-360`

Unsubscribe and session removal delete session IDs but never remove a URI key whose set becomes empty. Repeated
subscriptions to many unique URIs cause monotonic map growth.

**Resolution (2026-08-11)**: unsubscribe and session removal now use `computeIfPresent` and atomically remove an entry
when its session set becomes empty. Both paths are covered by a regression test.

### 25. STDIO server uses an unbounded cached request executor

**Evidence**: `aj-mcp-server/src/main/java/com/ajaxjs/mcp/server/ServerStdio.java:42-46,82-100`

Every input line is dispatched to `newCachedThreadPool()`. A fast or malicious peer can create an unbounded number of
threads while tools block, leading to memory exhaustion and scheduler collapse.

**Suggested direction**: use a bounded executor and queue with an explicit overload policy. Preserve enough concurrency
for cancellation/server responses, but cap accepted work.

## Cross-cutting test gaps to add with the fixes

1. Two sessions using the same request ID, with cancellation issued by only one session.
2. A POST SSE response that emits intermediate events before the final response and remains open between events.
3. GET SSE rejection (405) while an independent POST request succeeds.
4. GET SSE reconnect/resumption and server-side quiet-client disconnect cleanup.
5. Notification handler failure and invalid notification params producing no response.
6. Missing/untyped initialize fields and non-string JSON-RPC fields.
7. Tool registration with an unannotated parameter; null, invalid-list, fractional-integer, and overflow tool values.
8. Per-session logging thresholds with two clients.
9. Context-aware completion for 2025-06-18.
10. Session initialization without GET followed by transport close/DELETE.

## Suggested implementation order

1. Fix the remaining P0 item: request-scoped POST SSE.
2. Make Streamable HTTP session/GET lifecycle reliable (items 3-8).
3. Correct server request capability, timeout, and logging semantics (items 9-11).
4. Harden reflection registration/invocation and completion (items 12-16).
5. Address the remaining initialization validation, schema expressiveness, handler isolation, cache/API, and
   bounded-executor issues.
