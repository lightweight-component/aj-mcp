# Spring Streamable HTTP 示例

[English](README.md)

基于 Java 8、Spring Boot 2.7.18 和本地 AJ-MCP SDK，独立于原有示例。
`../spring` 和 `../tomcat` 继续演示旧 HTTP/SSE。不需要数据库、外部服务、API Key 或模型。

## 启动

在项目根目录执行，使用 JDK 8 或 17、Maven：

```sh
mvn -pl samples/server/spring-streamable-http,samples/client -am -DskipTests install
java -jar samples/server/spring-streamable-http/target/mcp-demo-spring-streamable-http-1.0.jar
```

默认地址 `http://127.0.0.1:8081/mcp`，可用 `--server.port=8082` 修改端口。
在 IDEA 中先运行 `com.foo.streamable.DemoApplication`，再运行 `samples/client` 中的
`com.foo.StreamableHttpClientExample`。客户端第一个启动参数可覆盖地址。
请先构建本地 SDK：相同版本号的已发布制品可能尚未包含最近的修复。

客户端会打印协商版本、工具、问候结果、三次进度、客户端 roots、欢迎资源和提示词。
roots 仅返回目录元数据，不读取或上传文件。通用客户端还支持
`http http://127.0.0.1:8081/mcp` 和 `auto http://127.0.0.1:8081/mcp`。

## HTTP 与生命周期

| 方法 | 用途 |
|---|---|
| POST `/mcp` | 初始化、请求、通知及反向响应 |
| GET `/mcp` | 异步 SSE，传递进度和服务端反向请求 |
| DELETE `/mcp` | 终止会话 |

控制器把协议处理交给 `ServerStreamableHttp`，复制其状态码、响应头和正文；通知不输出 null。
GET 使用 Servlet `AsyncContext`，不循环占用请求线程。完成、异常和超时按 writer 身份清理；
SDK 会话过期、连接替换、DELETE 和关闭会关闭 writer 并结束异步响应。
心跳与过期检测由 SDK 管理，Spring 在应用退出时关闭传输层。客户端在调用进度与 roots 工具前等待 GET 就绪。

`DemoTools` 演示无参数工具、可选参数、`_meta.progressToken` 和反向 `roots/list`。
注解扫描不会自动注入 Spring Bean，因此配置类显式接入工具依赖。
`RequestContext` 是实例内线程上下文，在 `finally` 清理，仅适用于同步 POST 线程，不能直接传给后台任务。

## 安全与边界

- 默认仅监听本机；原生客户端可不发 Origin，带 Origin 的浏览器请求默认拒绝。
- 用 `--sample.allowed-origins=https://app.example.com` 配置精确白名单。这不是认证或 CORS。
  跨域浏览器还需应用配置 CORS/预检和 MCP 响应头暴露策略。
- 这是本地教学示例，不是生产 TLS/认证网关，不要原样开放到公网。
- 集成测试覆盖 2025-03-26 和 2025-06-18；POST 返回 JSON，未实现服务端 POST-SSE、事件持久化重放或 batch。
- 浏览器直接 GET 未初始化的端点返回 404，请先通过 SDK 初始化。

## 测试

```sh
mvn -pl samples/server/spring-streamable-http -am -DskipTests=false -Dtest=StreamableHttpSampleTest -Dsurefire.failIfNoSpecifiedTests=false test
```

测试使用随机端口真实 Tomcat 和实际 SDK 客户端，覆盖两个版本、工具、进度、反向 roots、资源、提示词、
Origin 拒绝、版本头、通知空响应和 DELETE 清理，不需要提前启动服务器。
