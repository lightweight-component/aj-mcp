# AJ MCP Next（Java 8）

面向 MCP `2026-07-28` 的独立 SDK 工程，优先复用 AJ Util。
core 制品名为 `com.ajaxjs:aj-mcp-next-core`，版本继承 `ajaxjs-parent`；目录仍是 `aj-mcp-2026`。
已实现 core、client、server 三个模块。

| 模块                                       | 职责                                  | 依赖边界                                                                  |
|------------------------------------------|-------------------------------------|-----------------------------------------------------------------------|
| [mcp-core](mcp-core/README.zh-CN.md)     | 消息、编解码、方法 Schema、工具契约及无状态协议辅助操作     | AJ Util、Jackson 2、networknt Schema 校验器；无 HTTP、Servlet、Spring、旧 MCP 依赖 |
| [mcp-client](mcp-client/README.zh-CN.md) | 调用、超时、MRTR、订阅、STDIO/HTTP 客户端        | Core + 重构后的 AJ HTTP，无框架依赖                                             |
| [mcp-server](mcp-server/README.zh-CN.md) | 注册分发、请求生命周期、状态保护、STDIO/JDK HTTP 服务端 | 生产仅依赖 Core，client 仅用于测试                                               |

生产包使用 `com.ajaxjs.mcp2026`，可以与旧 SDK 同时存在。新工程不继承旧 SDK 的协议版本枚举或初始化流程。
根仓库 POM 已聚合此工程；也可以独立构建：

```sh
mvn -f aj-mcp-2026/pom.xml -DskipTests=false test
mvn -f aj-mcp-2026/pom.xml -DskipTests=false package
mvn -f aj-mcp-2026/pom.xml -DskipTests=false verify
```

以上命令从 aj-mcp 仓库根目录执行。进入此目录后可直接运行 `mvn -DskipTests=false test`。
代码按 Java 8 API 编译，构建不包含机器专属 JDK 路径。

## 本地 AJ Util 依赖

新核心依赖重构后的 `com.ajaxjs:ajaxjs-util:1.3.8`，使用 `com.ajaxjs.util.json.JsonEngine`。
如果本地 Maven 仓库中同版本制品较旧，请先在相邻 aj-util 仓库构建安装源码：

```sh
mvn -f ../aj-util/aj-util/pom.xml -DskipTests=true -Dmaven.test.skip=true install
mvn -f ../aj-util/aj-http/pom.xml -DskipTests=true install
```

这是依赖准备命令，跳过 AJ Util 测试，不代表已经验证整个工具库。
本工程显式声明 Jackson databind 和 jsr310 `2.22.1`，与当前 AJ Util POM 对齐，
因为 AJ Util 将它们声明为 `provided`。工程不修改全局 `JsonUtil` 引擎，也不复制通用 JSON 工具类。

## 支持范围

已实现：四类 JSON-RPC 消息、字符串/整数 ID、逐请求元数据、标准错误、基础能力检查、
严格 JSON 编解码、工具契约、离线 JSON Schema 校验、官方方法 Schema、发现/MRTR/订阅报文及关联检查。
详见 [core 职责清单](mcp-core/CORE-SCOPE.zh-CN.md)。

client/server 提供实际 STDIO、请求级 HTTP/SSE、有界执行与取消、注册发现、资源/提示词/工具路由、
completion 接入、MRTR 驱动、订阅派发、HTTP 头绑定及认证接入点，状态保护直接复用 AJ Util AES-GCM。

未提供旧协议自动回退、Tasks/任意扩展执行、共享响应缓存、注解扫描、OAuth 授权服务和业务防重放账本。
开放字段保留不代表执行对应功能。详见 [实现取舍与部署边界](IMPLEMENTATION-NOTES.zh-CN.md)。

client/server 各自包含传输适配，不引入协议 session。verify 还会运行打包后 JAR 的 STDIO 示例。
参考 [正式协议](https://modelcontextprotocol.io/specification/2026-07-28/basic/index) 和
[官方 Schema](https://modelcontextprotocol.io/specification/2026-07-28/schema)。

[English](README.md)
