<h1 align="center">AJ-MCP</h1>
<h3 align="center">A Lightweight Java MCP SDK</h3>

<div align="center" style="text-align: center;">

[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp)
![Java Version](https://img.shields.io/badge/Java-8-blue)
[![Javadoc](https://img.shields.io/badge/javadoc-API-brightgreen.svg?)](https://javadoc.io/doc/com.ajaxjs/aj-mcp )
![coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen.svg?maxAge=2592000)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/lightweight-component/aj-mcp)
![GitHub repo size](https://img.shields.io/github/repo-size/lightweight-component/aj-mcp)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/lightweight-component/aj-mcp)
[![Email](https://img.shields.io/badge/Contact--me-Email-orange.svg)](mailto:frank@ajaxjs.com)
[![中文](https://img.shields.io/badge/lang-中文-red)](./README.zh-CN.md)

</div>

<hr />

AJ-MCP is a lightweight Model Context Protocol (MCP) SDK for Java. It provides a small, direct API for building
MCP [clients](./aj-mcp-client) and [servers](./aj-mcp-server), especially for existing systems that must remain on Java
8 or Spring Boot 2.x.

- Supports Java 8 and above, and supports Spring Boot 2.x.
- Lightweight and small, with Jackson JSON and OkHttp as its main runtime dependencies.
- Easy to use and extend, with examples and bilingual documentation.
- Supports STDIO, the legacy HTTP/SSE transport, and Streamable HTTP.
- Implements MCP protocol revisions `2024-11-05`, `2025-03-26`, and `2025-06-18` with version negotiation.
- Covers tools, resources, resource templates, prompts, completion, pagination, subscriptions, cancellation, logging,
  roots, sampling, and elicitation.

Check out these three projects of MCP SDK components:

- [AJ MCP Common](./aj-mcp-common), Common library for AJ-MCP, it contains the implementation of the Model Context
  Protocol and some other common
  classes.
- [AJ MCP Client](./aj-mcp-client), MCP Client SDK.
- [AJ MCP Server](./aj-mcp-server), MCP Server SDK.
- [Samples](./samples) for both client and server.

For further information, please refer to the [User Manual](https://mcp.ajaxjs.com/) website.

## When to use AJ-MCP

AJ-MCP is intended for Java applications that need to expose existing business capabilities to AI clients, or consume
MCP servers without upgrading the entire application stack. Typical uses include:

- wrapping an existing Java service as MCP tools, resources, or prompts;
- connecting a Java desktop or server application to a local STDIO MCP process;
- integrating MCP into Spring Boot 2.x or a Servlet-based application;
- supporting clients that negotiate different MCP protocol revisions.

JSON-RPC batch messages are intentionally not supported.

## Modules

| Module                             | Purpose                                                                                            | Current version |
|------------------------------------|----------------------------------------------------------------------------------------------------|-----------------|
| [`aj-mcp-common`](./aj-mcp-common) | Shared JSON-RPC messages, MCP protocol models, content types, version metadata, and JSON utilities | `1.7`           |
| [`aj-mcp-client`](./aj-mcp-client) | Synchronous client API and STDIO, HTTP/SSE, and Streamable HTTP transports                         | `1.5`           |
| [`aj-mcp-server`](./aj-mcp-server) | Annotation-based feature discovery, request dispatch, errors, sessions, and server transports      | `1.4`           |
| [`samples`](./samples)             | STDIO, Spring Boot/SSE, and embedded Tomcat examples                                               | —               |

The client and server artifacts already depend on `aj-mcp-common`; applications normally add only the artifact they use.

## Quick start

### Expose a tool from a Java 8 server

Add the server dependency:

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.4</version>
</dependency>
```

Declare a service and start it over STDIO:

```java

@McpService
public class GreetingTools {
    @Tool(description = "Greets a user")
    public String greet(@ToolArg("name") String name) {
        return "Hello, " + name;
    }
}

public static void main(String[] args) {
    FeatureMgr features = new FeatureMgr();
    features.init("com.example.mcp");

    McpServer server = new McpServer();
    server.setFeatureMgr(features);
    server.setServerConfig(new ServerConfig());
    server.setTransport(new ServerStdio(server));
    server.start();
}
```

Keep standard output reserved for JSON-RPC messages when using STDIO; send application logs to standard error or a log
file. See the [server README](./aj-mcp-server/README.md) for tools, resources, prompts, and HTTP transports.

### Connect from a Java client

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-client</artifactId>
    <version>1.5</version>
</dependency>
```

```java
try(McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/path/to/mcp-server.jar")){
        client.

listTools().

forEach(tool ->System.out.

println(tool.getName()));
String result = client.callTool("greet", "{\"name\":\"AJ-MCP\"}");
    System.out.

println(result);
}
```

Always close a client so its streams, HTTP connections, pending requests, worker threads, and optional child process are
released. See the [client README](./aj-mcp-client/README.md) for HTTP transports and additional APIs.

## Build from source

Use Maven with JDK 8 or JDK 17:

```bash
mvn test
```

To build only one module and its dependencies:

```bash
mvn -pl aj-mcp-client -am test
mvn -pl aj-mcp-server -am test
```

## Why MCP?

The Model Context Protocol opens up exciting possibilities for building intelligent applications using your application
data with your favourite
programming language and framework. With AJ MCP Servers, you have a
powerful foundation to create your own Java based servers that can bridge AI with any data source or system you can
imagine.

Whether you want to connect to your favorite database, integrate with your company’s internal systems, or build
something completely new - the sky
truly is the limit! The simplicity of implementing MCP servers with AJ
means you can focus on the creative aspects rather than the plumbing.

So what are you waiting for? Grab the code, fire up your IDE, and start building your own MCP server today. The future
of AI-powered applications is
here, and you can be part of shaping it!

Have Fun!

## Links

[User Manual](https://mcp.ajaxjs.com/) | [Github](https://github.com/lightweight-component/aj-mcp) | [Gitcode](https://gitcode.com/lightweight-component/aj-mcp)

## Helpful links for MCP

[Awesome MCP Servers](https://mcplab.cc/zh) | [TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk) | [MCP Documentation](https://modelcontextprotocol.io)
