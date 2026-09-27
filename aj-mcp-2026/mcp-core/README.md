# mcp-core

Maven coordinates: `com.ajaxjs:aj-mcp-next-core:1.36` (version inherited from `ajaxjs-parent`; see the POM).
A Java 8 protocol foundation using AJ Util's `JsonEngine`, without threads, network connections or global sessions.

## API responsibilities

| Type                                         | Purpose                                                                       |
|----------------------------------------------|-------------------------------------------------------------------------------|
| `RpcRequest` / `RpcNotification`             | Separate requests from ID-less notifications                                  |
| `RpcSuccess` / `RpcError`                    | Separate result responses from protocol errors                                |
| `RequestId`                                  | String or exact integer; numeric `1` differs from textual `"1"`               |
| `RequestMetadata`                            | Per-request version, capabilities, optional clientInfo and extension `_meta`  |
| `McpCodec`                                   | JSON and UTF-8 encoding/decoding; never sends a response automatically        |
| `ProtocolValidator`                          | Supported-version and required-capability-subtree checks                      |
| `McpResult` / `ToolResults`                  | `complete`, `input_required`, text and structured tool results                |
| `ToolDefinition`                             | Preserves full input/output schemas and extension fields                      |
| `SchemaValidator` / `DefaultSchemaValidator` | Offline JSON Schema compilation and instance validation                       |
| `ProtocolSchema`                             | Official method/content schemas, correlation and per-request semantic checks  |
| `ToolContract`                               | Compile tool schemas once and validate calls and successful structured output |

Example types are in `com.ajaxjs.mcp2026.core`; the Schema SPI is in `.schema`.
Open fields use Maps/Lists/strings/numbers/booleans/null, deep-copied to immutable snapshots at construction.
Use AJ Util to convert beans to JSON values beforehand. Serialize models with `McpCodec`, not direct bean serialization.

## Construct and read a tool request

```java
import com.ajaxjs.mcp2026.core.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

McpCodec codec=new McpCodec();
        RequestMetadata metadata=RequestMetadata.create(Collections.emptyMap());
        Map<String, Object> params=new LinkedHashMap<>();
        params.put("name","search");
        params.put("arguments",Collections.singletonMap("q","Java 8"));

        RpcRequest request=RpcRequest.create(RequestId.of("call-1"),"tools/call",params,metadata);
        String json=codec.encode(request);
        RpcRequest received=(RpcRequest)codec.decode(json);
        ProtocolSchema protocol=new ProtocolSchema();
        protocol.requireRequest(received);

        RpcSuccess response=new RpcSuccess(received.getId(),ToolResults.text("Found 3 items"));
        protocol.requireResult(received,response);
        byte[]responseBytes=codec.encodeUtf8(response);
```

The params supplied to `create` must not contain `_meta`, preventing silent replacement of conflicting metadata.
`clientInfo` describes software, not authenticated identity. An unsupported version still decodes;
`requireSupportedVersion` then produces `-32022` with `data.supported` / `data.requested`.
Callers must perform this check before dispatch. Capability checks use each request's own metadata.

## Results and MRTR

```java
McpResult failedTool = ToolResults.error("Invalid business input");
McpResult structured = ToolResults.structured(
        Collections.emptyList(), java.util.Arrays.asList("a", "b"), false);
McpResult needsRetry = McpResult.inputRequired(null, "opaque-request-state");
```

Tool business failures use `RpcSuccess` with `isError=true`, distinct from `RpcError`.
`structuredContent` accepts any JSON value, preserving explicit null.
`input_required` needs at least `inputRequests` or `requestState`; the latter is an opaque string.
These APIs construct/read results, but do not execute input interactions, automatic retries or persistence.
A retry can carry `inputResponses` and `requestState` in its params.

## Codec contracts

- Modern `2026-07-28` handling only; batches are rejected. No initialize or session state.
- The default reuses AJ Util's `Jackson2Engine`, specializing inbound parsing: duplicate keys, trailing
  documents, invalid UTF-8 and nonstandard JSON are rejected. Decimals use BigDecimal and integers BigInteger.
- Default document limit: 4 MiB of UTF-8 bytes, configurable in the constructor. The default engine also
  limits nesting to 64, numeric length to 1000, and string length to 4 Mi characters. Snapshots enforce depth 64.
- Size violations produce local `IllegalArgumentException`; parsing/envelope failures produce
  `McpProtocolException`. Wire error messages contain no raw input. Causes retain local parser diagnostics
  and must not be exposed directly to peers.
- For a recognized failed notification, `toErrorResponse()` returns null. Do not serialize/send that null;
  future transports handle HTTP status separately. Clients report malformed responses locally rather than
  replying with another error to the peer.
- Only `complete` / `input_required` are accepted by default. Explicitly configure extension result types
  after ensuring the extension is enabled. A missing `resultType` is not filled in as for legacy revisions.
- Error codes use Java int, preserving unknown incoming 32-bit codes. Encoding rejects deprecated and
  unallocated MCP-reserved codes. Uncorrelated modern errors omit ID.
- An injected `JsonEngine` is retained per codec and is unaffected by `JsonUtil.setEngine`.
  It must implement strict parsing, exact numeric handling and safe concurrent use; the codec does not
  reconfigure arbitrary engines.
- Finish default-engine configuration before use. Do not mutate the inherited ObjectMapper or a shared engine.

## Validation boundaries

The codec validates envelopes and required metadata shape, preserving open method params,
result extensions, capability extensions and schema fields. Method validation is explicit, not implicit in decoding.
`ToolDefinition` checks the required object input schema, preserving `$defs`, `oneOf`, `$ref` and other
keywords; it does not resolve references, validate arguments or prove schema validity.

`DefaultSchemaValidator` uses networknt 2.0.4 (Java 8/Jackson 2), with YAML excluded. It defaults to
2020-12 and also accepts explicit `http://json-schema.org/draft-07/schema#`. Other dialects fail explicitly.
It validates the schema against its bundled meta-schema before compilation and eagerly resolves references.
There is no remote fetch option: local references work; unresolved network/file/classpath references fail.
Only bundled standard meta-schemas may be loaded. Schema snapshots and compiled plans can be reused concurrently.
Default limits: depth 64, 20,000 JSON nodes, 4 Mi aggregate key/string/number characters and 1,000 characters
per number. Depth/node limits are configurable. Simple non-progressing local pointer cycles are rejected.
These are structural limits, not a hard CPU timeout or a sandbox for hostile regexes; regex behavior follows
the engine's JDK implementation. `format` follows dialect defaults (annotation-only in 2020-12).
Diagnostics contain paths/keywords, not rejected values; exception causes are local diagnostics only.

Use one reusable `ProtocolSchema` to `requireRequest` before dispatch and `requireResult` on correlated
responses. It covers all ten core client methods and the bundled content/notification/MRTR definitions.
Use `requireClientNotification` for inbound client notifications, `requireNotification` for general
notification shape, and `requireRequestNotification` for progress tokens, log thresholds and server cancellation
restrictions. Notification failures have a null `toErrorResponse`; response validation errors are local and
must not be sent to the peer. Unknown extension methods/results need an extension validator, not a permissive fallback.

For tools, create `new ToolContract(tool, new DefaultSchemaValidator(), protocol)` once per registered tool.
Call `requireCall(request)` before invocation and `requireResult(request, response)` afterwards. Omitted
arguments become an empty object; invalid arguments produce `INVALID_PARAMS`. Successful output is checked
against `outputSchema`; an explicit JSON null differs from an absent `structuredContent`. Tool execution errors
remain `isError=true` results. Schema-invalid registration fails before invocation.

## Discovery messages

`Discovery` supplies core wire helpers for the mandatory `server/discover` method,
without creating connections, sessions, or caches.

```java
RpcRequest request = Discovery.request(RequestId.of(1),
        RequestMetadata.create(Collections.emptyMap()));
McpResult result = Discovery.result(Collections.emptyMap(), "example-server", "1.0");
RpcSuccess response = new RpcSuccess(request.getId(), result);
Discovery.validate(response.getResult());
```

The convenience factory defaults to `ttlMs=0` and `cacheScope=private`. For custom
cache hints, version lists, or `instructions`, use `McpResult.complete(fields)`
and then `Discovery.validate`. Checks cover required fields, version array items,
cache scope, non-negative integer TTL, and required fields of optional server identity;
unknown extensions are preserved. The generic codec cannot infer a response's
method: clients should invoke the validator based on the original request.
Private caches must be isolated by authorization context; no cache or authorization
policy is implemented here.

This core does not by itself establish full SDK conformance. Server dispatch, authentication,
transport rules and client execution belong in their respective modules; see [responsibility checklist](CORE-SCOPE.md).

## Multi-round follow-up requests

`MultiRoundTrip.retry(original, response, nextId, inputResponses)` constructs but
never sends a follow-up. It supports `tools/call`, `resources/read`, and `prompts/get`,
checking response correlation, a different request ID, `input_required`, and complete
inputs for the current round. Original arguments and metadata are retained; only
current inputs and opaque state are replaced. State is never parsed and stale state
from an earlier round is removed.

For these methods, `RpcRequest` checks that `requestState` is a string and
`inputResponses` is an object with object values. Decoding failures become
`INVALID_PARAMS` with the request ID. Generic decoding allows partial inputs so a
server can request missing information again. Additional inputs are preserved for
the server to ignore when unnecessary.

`ProtocolSchema.requireResult` checks requested input shapes and per-request capabilities.
`requireRetry(original, inputRequired, retry)` additionally checks preserved arguments/state, corresponding
input result types, and accepted form content against its requested schema. This is not an execution engine:
timeouts, cancellation, round limits, state integrity and replay protection remain client/server responsibilities.
Clients echo state unchanged; servers must not trust it. Inbound partial inputs can still trigger another round.

## Subscription messages

`Subscriptions.listen(id, metadata, filter)` constructs a listen request. `RpcRequest`
checks the required notifications object, built-in boolean flags and resource string
array. Empty filters are valid and unknown extensions survive.
`Subscriptions.acknowledge(request, accepted)` constructs the opening notification,
checking that accepted core types and resources are a subset of those requested.
Extension negotiation and filter semantics remain the extension implementation's responsibility.
`notificationId(notification)` checks correlation metadata and, for acknowledgments,
filter shape. `closed(request)` constructs graceful completion;
`validateClosed(request, response)` checks its type and both correlation IDs.

`ProtocolSchema.requireSubscriptionNotification` checks accepted core notification types and IDs.
Resource matching defaults to exact URI equality; its predicate overload lets the resource provider define
valid sub-resource relationships without unsafe prefix guessing. Acknowledgment is a notification, not a
completion response; completion is sent only at graceful closure. Ordering, capability support,
cancellation, transport closure and reconnection remain client/server responsibilities.
The generic codec does not infer subscription responses or stream notifications;
callers must apply the correlation checks above.

## Build and tests

Shared pure helpers `HttpMetadata` and `WireIO` provide header/body binding, x-mcp-header validation,
safe header encoding and bounded UTF-8 stream framing. They open no connections, spawn no threads and own no streams.
Client/server own all actual transport lifecycle and execution.

First install the refactored local AJ Util as described in the [project README](../README.md), then run from
the repository root:

```sh
mvn -pl aj-mcp-2026/mcp-core -am -DskipTests=false test
mvn -f aj-mcp-2026/pom.xml -DskipTests=false package
```

Tests cover the real AJ Util engine, JSON boundaries, ID precision, per-request isolation, immutable snapshots,
notification error suppression, extension-result policy, method schemas, tool contracts, offline references,
MRTR correlation/capabilities, subscription filtering and concurrent validation.

[中文](README.zh-CN.md)
