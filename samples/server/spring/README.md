# Spring Boot 2.x MCP Server over SSE

This is a Spring Boot 2.7 web application with AJ-MCP Server running over the legacy HTTP/SSE transport. It demonstrates
how to keep the MCP implementation framework-neutral while adapting it to Spring MVC controllers.

[中文](./README.zh-CN.md)

## What the sample contains

- `Config`: scans `com.foo.myapp` for `@McpService` features and creates `McpServer` plus `ServerSse` beans.
- `SseController`: exposes `GET /sse`, opens a session, and advertises its message endpoint.
- `MessageController`: exposes `POST /message?uuid=...` and routes JSON-RPC messages back to the originating session.
- `mcp/McpServerTools`, `McpServerResources`, and `McpServerPrompts`: example MCP capabilities, including image/binary
  content that has no external-file dependency and a URI-template greeting resource.
- `Sse2Controller`: a standalone Spring `SseEmitter` demonstration at `/ss2`; it is not the MCP endpoint.

## Run

Requirements: JDK 8 or newer and Maven.

From this directory:

```bash
mvn spring-boot:run
```

Alternatively, run the main method in `com.foo.myapp.DemoApplication` from the IDE.

The MCP SSE endpoint is:

```text
http://localhost:8080/sse
```

Connect with `HttpMcpTransport`; the server sends an `endpoint` event that points the client to
`/message?uuid=<session-id>`:

```java
McpTransport transport = HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .build();

try (McpClient client = McpClient.builder().transport(transport).build()) {
    client.initialize();
    client.listTools().forEach(tool -> System.out.println(tool.getName()));
}
```

## Integration notes

- Keep each response session-scoped by calling `serverSse.handle(uuid, json)`, not `broadcast()`.
- Remove the connection when the servlet request ends; the sample does this in `finally`.
- In a production application, close `ServerSse` from a Spring destruction hook so its heartbeat executor and sessions
  are released.
- This sample demonstrates the legacy HTTP/SSE transport. For a new HTTP integration, prefer `ServerStreamableHttp`
  where client compatibility allows it.
- `ServerSse` is a Spring bean with `close()` configured as its destruction method, so the heartbeat executor is released
  on normal Spring shutdown.
