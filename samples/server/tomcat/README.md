# Example for MCP Server in SSE

Here is a simple example of how to use MCP in Java to implement a server that sends messages over Server-Sent Events (
SSE).

[中文](./README.zh-CN.md)

- Java 8
- Tomcat 9, a standalone JAR application without Spring

The sample embeds Tomcat 9 and adapts AJ-MCP's legacy `ServerSse` transport with two servlets:

- `GET /sse` opens the event stream and advertises a session-specific message endpoint.
- `POST /message?uuid=...` accepts JSON-RPC messages and sends the response only to that session.

MCP tools, resources, and prompts are discovered from the `com.foo.myapp` package. The feature set includes a
self-contained PNG, binary/text resources, and a URI-template greeting resource.

## Build and run

Requirements: JDK 8 or newer and Maven.

```bash
mvn package
java -jar target/mcp-demo-tomcat-1.0.jar
```

If the shaded artifact name differs in your Maven environment, run the non-original JAR produced under `target/`.

The MCP endpoint is:

```text
http://localhost:8080/sse
```

Connect with the legacy client transport:

```java
McpTransport transport = HttpMcpTransport.builder()
        .sseUrl("http://localhost:8080/sse")
        .build();

try (McpClient client = McpClient.builder().transport(transport).build()) {
    client.initialize();
    System.out.println(client.listTools());
}
```

## How it is wired

`StandaloneTomcat` creates `FeatureMgr`, `McpServer`, and `ServerSse`, then registers `SseServlet` and `MessageServlet`
programmatically. No Spring container or `web.xml` is required.

For production use, add an application shutdown hook that closes `ServerSse`, validate request sizes and authentication
at the HTTP layer, and configure timeouts for the expected lifetime of SSE connections.
