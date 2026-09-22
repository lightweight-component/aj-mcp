# Spring Streamable HTTP sample

[中文](README.zh-CN.md)

Java 8 + Spring Boot 2.7.18, using the local AJ-MCP SDK. Existing `../spring` and `../tomcat`
remain legacy HTTP/SSE examples. No database, external service, API key, or model is required.

## Run

From the repository root with JDK 8 or 17 and Maven:

```sh
mvn -pl samples/server/spring-streamable-http,samples/client -am -DskipTests install
java -jar samples/server/spring-streamable-http/target/mcp-demo-spring-streamable-http-1.0.jar
```

The endpoint is `http://127.0.0.1:8081/mcp`; override the port with `--server.port=8082`.
In IDEA, run `com.foo.streamable.DemoApplication`, then `com.foo.StreamableHttpClientExample` in
`samples/client`. The client's optional first argument overrides the endpoint. Build the local SDK first:
published artifacts with the same version may not contain recent fixes.

The client prints the negotiated revision, tools, greetings, three progress messages, client roots,
the welcome resource, and a prompt. Roots contain metadata only; the sample never reads or uploads files.
The generic client also accepts `http http://127.0.0.1:8081/mcp` and `auto http://127.0.0.1:8081/mcp`.

## HTTP and lifecycle

| Method | Purpose |
|---|---|
| POST `/mcp` | Initialize, requests, notifications, and reverse responses |
| GET `/mcp` | Asynchronous SSE stream for progress and server-initiated requests |
| DELETE `/mcp` | Terminate the session |

The controller delegates protocol handling to `ServerStreamableHttp`, copying status, headers and body.
Notifications have no body, not a serialized null. GET uses Servlet `AsyncContext`, not a sleeping request thread.
Completion/error/timeout detaches the exact writer. SDK expiry, replacement, DELETE and shutdown close the writer
and complete its asynchronous response. The SDK owns heartbeats and idle expiry; Spring closes it on shutdown.
The client waits for GET readiness before invoking progress or roots tools.

`DemoTools` covers a parameterless tool, optional arguments, `_meta.progressToken` and reverse `roots/list`.
Annotation scanning does not inject Spring beans, so configuration explicitly wires the scanned tool instance.
`RequestContext` is instance-owned, cleared in `finally`, and only valid on the synchronous POST thread.

## Security and scope

- Loopback only by default; native clients may omit Origin. Supplied browser origins are rejected by default.
- Set exact origins using `--sample.allowed-origins=https://app.example.com`. This is not authentication or CORS.
  Browser cross-origin access additionally needs a CORS/preflight policy and exposed MCP response headers.
- This is a local teaching sample, not a production TLS/authentication gateway. Do not expose it publicly as-is.
- Integration tests cover 2025-03-26 and 2025-06-18. POST returns JSON; server POST-SSE streaming,
  persisted event replay and JSON-RPC batches are not implemented.
- Opening GET in a browser without initialization returns 404; initialize through the SDK first.

## Test

```sh
mvn -pl samples/server/spring-streamable-http -am -DskipTests=false -Dtest=StreamableHttpSampleTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Tests use real embedded Tomcat on a random port and the actual SDK client. They cover both revisions, tools,
progress, reverse roots, resources/prompts, Origin rejection, version headers, empty notifications and DELETE cleanup.
