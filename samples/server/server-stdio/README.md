# MCP Server STDIO Sample

This sample packages an annotation-based AJ-MCP server as a standalone executable JAR that communicates through standard
input and standard output.

[中文](./README.zh-CN.md)

It demonstrates:

- tools with strings, numbers, booleans, no arguments, errors, images, and multiple content items;
- text and binary resources, including a URI-template greeting resource;
- basic, parameterized, multi-message, image, and embedded-resource prompts;
- package scanning and STDIO server startup.

## Build and run

```bash
mvn package
java -jar target/my-app-jar-with-dependencies.jar
```

The process waits for newline-delimited JSON-RPC requests on standard input. It is normally started by an MCP client
rather than used interactively:

```java
try (McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/absolute/path/to/my-app-jar-with-dependencies.jar")) {
    client.listTools().forEach(tool -> System.out.println(tool.getName()));
}
```

Standard output is the protocol channel and must contain only JSON-RPC messages. Send logs to standard error or a file.

`App` already assigns the scanned `FeatureMgr` to the `McpServer` before startup. The sample enables the current strict
MCP lifecycle, so clients should initialize before listing or calling features. Try `demo://welcome`, `demo://image`,
or a template URI such as `demo://greeting/Ada` after initialization.
