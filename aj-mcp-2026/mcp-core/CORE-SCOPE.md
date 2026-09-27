# aj-mcp-next-core responsibility and verification checklist

This round completes the three-module design's **core: messages, codec, structural validation and stateless protocol
helpers**.
It is not a network-ready client/server, and passing core tests is not full SDK conformance certification.

| Responsibility                            | Core provides                                                                                  | Integration / runtime responsibility                                                |
|-------------------------------------------|------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| JSON-RPC, UTF-8, notification suppression | Codec, immutable messages, null error response for notifications                               | Transport must not write null responses; STDIO stdout is protocol-only              |
| HTTP metadata / framing                   | HttpMetadata and WireIO: pure header binding and bounded UTF-8 stream helpers                  | Client/server open, manage and close actual connections/streams                     |
| Per-request version/capabilities          | Metadata, ProtocolValidator and ProtocolSchema                                                 | Check each request, not connection-cached negotiation context                       |
| Errors                                    | Standard errors, unknown inbound codes preserved, forbidden outbound codes rejected            | Dispatch maps business errors; never respond to invalid peer responses              |
| Ten core client methods                   | Bundled official request/result schemas                                                        | Register and execute advertised tools/resources/prompts without overstating support |
| JSON Schema                               | 2020-12, explicit draft-07, meta-validation, offline references, structural limits             | Compile at registration and reuse; reject other dialects or inject a validator      |
| Tool inputs/outputs                       | ToolContract                                                                                   | Service authorization, throttling and side effects                                  |
| Discovery/cache fields                    | Discovery, non-negative integer TTL, public/private shapes                                     | Client expiry and authorization-context cache isolation                             |
| MRTR                                      | Result/follow-up messages, input types/capabilities/form validation                            | Client interactions and round/time limits; server state integrity/replay protection |
| Subscriptions                             | Listen/ack/close, IDs/filters/accepted notification checks                                     | Ack-first ordering, registration, delivery, cancellation, closure/reconnection      |
| Progress/logs/cancellation                | Notification schemas, progress tokens, log thresholds, server cancellation method restrictions | In-flight status, monotonic progress and STDIO/HTTP-specific rules                  |
| Optional extensions                       | Open fields preserved, explicit codec result-type configuration                                | Extension negotiation and semantics, separate from strict core routing              |

## Integration sequence

1. Decode using the codec: envelope checks only, no implicit schema engine initialization.
2. Use `protocol.requireRequest` before dispatch; create ToolContract at registration and use `requireCall` before
   invocation.
3. Use `protocol.requireResult` or ToolContract's `requireResult`, then encode the result.
4. Clients correlate and validate responses against the original request. Validate notifications by direction/context
   without responding.
5. Build continuations with `MultiRoundTrip.retry` and check `protocol.requireRetry`; apply subscription
   filter/correlation checks on streams.

## Verification and limits

- Core tests/builds run on actual JDK 8 and JDK 17, plus module-selected root builds.
- Tests cover offline references, productive local recursion, trivial infinite recursion rejection, schemas/instances,
  tools, MRTR, subscriptions and concurrency.
- networknt 2.0.4 adds the `itu` date-format dependency; no YAML, HTTP, Servlet or Spring dependency is introduced.
- Structural limits are not hard real-time isolation for hostile schemas. Hosts must consider regex/composition CPU
  cost.
  In 2020-12, `format` is annotation-only by default. This is not a claim of passing the entire official JSON Schema
  suite.
- Targets 2026-07-28 only. Legacy negotiation/adaptation, extension implementations and transport conformance are
  outside this core.

## Official schema snapshot

Runtime reads only the bundled JAR resource, never the website or a moving main branch.
Origin, retrieval date, digest and license are recorded in `META-INF/MCP-SCHEMA-NOTICE.txt`
and `MCP-SCHEMA-LICENSE.txt`.
Protocol upgrades require explicit resource updates, prose-rule review and tests, not just a version constant change.

[中文](CORE-SCOPE.zh-CN.md)
