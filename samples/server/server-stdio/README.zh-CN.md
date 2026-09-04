# MCP Server STDIO 示例

本示例把基于注解的 AJ-MCP 服务打包成独立可执行 JAR，通过标准输入和标准输出通信。

[English](./README.md)

示例包括：

- 字符串、数字、布尔值、无参数、异常、图片和多内容返回等工具；
- 文本、二进制资源及 URI 模板问候资源；
- 基础、带参数、多消息、图片及内嵌资源提示词；
- 包扫描和 STDIO 服务启动。

## 构建与运行

```bash
mvn package
java -jar target/my-app-jar-with-dependencies.jar
```

进程会从标准输入等待逐行 JSON-RPC 请求，通常由 MCP 客户端启动，而不是手工交互：

```java
try (McpClient client = McpClient.createStdioMcpClient(
        "java", "-jar", "/absolute/path/to/my-app-jar-with-dependencies.jar")) {
    client.listTools().forEach(tool -> System.out.println(tool.getName()));
}
```

标准输出是协议通道，只能包含 JSON-RPC 消息。日志应写入标准错误或文件。

`App` 已在启动前把扫描到的 `FeatureMgr` 设置到 `McpServer`。示例启用了当前 MCP 的严格生命周期，客户端必须先初始化再列举或调用功能。
初始化后可读取 `demo://welcome`、`demo://image`，或 `demo://greeting/Ada` 这样的模板资源。
