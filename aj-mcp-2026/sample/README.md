# Run your first MCP tool

`server` is a Spring Boot application providing tools. `client` is a standalone Java application calling them. Both target Java 8 and MCP `2026-07-28`. No database or model API key is required.

Source: [GitHub](https://github.com/lightweight-component/aj-mcp), [Gitee](https://gitee.com/lightweight-components/aj-mcp), [GitCode](https://gitcode.com/lightweight-component/aj-mcp). These examples live in `aj-mcp-2026/sample`.

## Dependencies

Use `com.ajaxjs:aj-mcp-next-server:1.0` for a server and `com.ajaxjs:aj-mcp-next-client:1.0` for a client. Both bring in core transitively. The sample server uses `spring-boot-starter:2.7.18`, following the repository's Java 8 baseline. Its POM aligns Jackson dependencies to the SDK version so Boot dependency management does not downgrade them.

## Build and run

Run from the repository root with Maven and JDK 8 or 17. Maven dependencies must be available locally or downloadable.

```sh
git clone https://github.com/lightweight-component/aj-mcp.git
cd aj-mcp
mvn -f aj-mcp-2026/pom.xml -pl sample/server,sample/client -am -DskipTests package
```

This builds the SDK from the current checkout along with both examples. Tests are skipped for this quick packaging command. To prepare local dependencies for separate projects, run `mvn -f aj-mcp-2026/pom.xml -DskipTests install`.

Start the server in one terminal:

```sh
java -jar aj-mcp-2026/sample/server/target/aj-mcp-next-sample-server-1.0.jar
```

Run the client in another:

```sh
java -jar aj-mcp-2026/sample/client/target/aj-mcp-next-sample-client-1.0.jar
```

The client prints discovery data, the `echo` and `query_order` tool names, a greeting, and the shipment status of mock order `A1001`. Tool descriptions and sample output use Chinese.

Pass an unknown order ID to see a business error:

```sh
java -jar aj-mcp-2026/sample/client/target/aj-mcp-next-sample-client-1.0.jar http://127.0.0.1:8080/mcp UNKNOWN
```

To change the port, start the server with `--mcp.port=8081` and pass `http://127.0.0.1:8081/mcp` as the client's first argument. Stop the server with Ctrl+C. Spring closes the HTTP transport before the dispatcher; the client closes its resources and exits after its calls.

## Read the code

Start with `SampleServerApplication`: `mcpServer()` defines the tools and registers handlers. Replace the mock order lookup with your application's service call. `mcpHttpTransport()` exposes the SDK endpoint at `http://127.0.0.1:8080/mcp`.

Spring Boot manages the beans; the SDK's JDK HTTP adapter handles requests. There is no MVC controller or additional Tomcat port. Configure the port with `mcp.port`. Integrating with an existing Spring MVC endpoint would require a separate adapter using `server.connect(...)`.

`SampleClient` discovers the server, lists tools, and calls both tools. Its independent executable JAR helps verify the server before connecting an AI application.

A browser GET to `/mcp` returns 405; use the supplied MCP client to make POST requests. Other clients must support the same protocol revision. The endpoint binds to loopback without authentication for local learning; remote hosting requires authentication, HTTPS, and suitable network limits.

[中文](README.zh-CN.md)
