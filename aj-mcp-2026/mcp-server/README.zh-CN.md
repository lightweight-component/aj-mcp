# AJ MCP Next Server

兼容 Java 8；生产依赖仅 core，并传递复用 AJ Util。client 仅作为测试依赖。
注册/分发、STDIO、JDK HTTP 适配器均位于本模块，不另拆 transport 工程。

## 最小内嵌 HTTP 服务

```java
Map<String, Object> schema=new LinkedHashMap<>();
        schema.put("type","object");
        schema.put("properties",Collections.singletonMap("text",Collections.singletonMap("type","string")));
        schema.put("required",Collections.singletonList("text"));
        McpServer server=new McpServer("example","1");
        server.tool(ToolDefinition.create("echo",schema),context->{
        Map<?, ?> arguments=(Map<?, ?>)context.request().getParams().get("arguments");
        return ToolResults.text((String)arguments.get("text"));
        });
        HttpServerTransport http=new HttpServerTransport(server,8080);
        http.start();
// Keep your application running; at application shutdown:
        http.close();
        server.close();
```

导入 `com.ajaxjs.mcp2026.server.*`、`com.ajaxjs.mcp2026.core.*`、`java.util.*`。
真实应用启动后不要立即关闭 transport；在宿主生命周期结束时关闭。
STDIO 可运行 [EchoServer](src/main/java/com/ajaxjs/mcp2026/server/example/EchoServer.java)，
`StdioServerTransport.run()` 阻塞至 EOF/关闭。stdout 只能写协议 JSON，日志必须走 stderr。

## 注册与协议流程

- `tool(definition, handler)`：注册时编译 Schema，调用前校验入参，返回时校验结构化输出。
- `resource(descriptor, handler)` / `prompt(descriptor, handler)`：描述深度快照，自动列表及路由。
  校验 prompt 必填参数；资源读取结果需要 contents、ttlMs、cacheScope。
- `method(name, handler)`：覆盖已知核心方法，接入资源模板、分页、按身份过滤的列表或自动补全。
  自定义 handler 负责其 provider 专属校验与授权。
- 内置列表按注册 key 排序，单页、TTL 0/private，未知游标拒绝。
- `server/discover` 是普通请求，不建立协议会话；每次请求检查版本和 metadata。
- 注册 completion handler 后才声明 completions 能力。支持空资源模板列表；
  URI 模板解析可通过 method 接入自定义 provider。
- 工具业务失败可以返回 `ToolResults.error`；非法请求返回协议错误。
  未处理业务异常转换为不泄露细节的 InternalError；可配置错误回调接收原始异常/cause。
- Context 提供 request、可信 Principal、取消检测、进度和逐请求日志。
  handler 应采用可中断 I/O 或 checkCancelled()，不依赖协议 session 存储状态。
- 注册表变更触发已订阅的列表通知；resourceUpdated(uri) 按完整 URI 匹配资源订阅。
  只确认已请求的核心过滤器，不把未知扩展过滤器冒充已实现能力。

## 生命周期与部署

默认 8 个业务 worker、128 个排队任务；请求执行上限 30 秒，订阅生命周期 5 分钟。
每连接最多 128 个在途请求；重复在途 ID 会关闭该连接，避免响应歧义。
超时/关闭中断业务并抑制迟到输出。订阅先 ack 后通知，有限生命周期结束时返回完成结果；
服务端关闭时通过 STDIO 取消活动订阅。notification 始终没有 RPC 响应。

HTTP 默认绑定 127.0.0.1、路径 /mcp；携带 Origin 时必须匹配显式白名单。
要求 POST + JSON，请求 Accept 包含 JSON/SSE；GET/DELETE 返回 405；
头与正文不一致返回 HTTP 400/-32020。成功请求各自使用 SSE 响应，心跳注释用于探测断线取消。
没有会话粘滞、GET 事件通道。关闭 adapter 不会自动关闭可复用的 server。
HTTP 默认 32 个 worker、128 个排队任务，每个活动订阅占用一个 HTTP worker。

扩展构造器支持地址/路径/Origin 和 Authentication 回调。
回调验证凭据后返回可信 Principal，或抛 SecurityException（401）。
默认本地示例未认证；Origin 校验既不是认证，也不是 CORS 策略。
远程部署必须补充认证、TLS 终止、请求体/请求头/读写时间限制和限流。
JDK HttpServer/HttpURLConnection 是阻塞式适配器，库不修改 JVM 全局 HTTP 配置，
也不承诺卡住的 OS/socket 写操作具有硬实时退出保证。协议超时不能替代宿主/代理的网络超时。
需要更大并发时，可以通过 server.connect(sink, principal, true) 接入现有 Web 框架。

## MRTR 状态

handler 返回 McpResult.inputRequired(inputs, opaqueState)，由 client 执行交互。
发送前会检查请求输入所需的能力及响应结构。可用 RequestState 和部署管理的随机 128 位密钥：

```java
RequestState states=new RequestState(RequestState.newKey(),"my-service");
// First round:
        String token=states.seal(context.request(),context.principal(),
        Collections.singletonMap("stage","confirm"),60_000);
// Continuation:
        Map<String, Object> state=states.open(context.request(),context.principal());
```

直接复用 AJ Util AES-GCM，每次使用随机 nonce。token 有过期/大小限制，并绑定服务 audience、
可信身份、方法及原始参数/metadata（排除 inputResponses、requestState、ID）。
多副本应共享部署密钥；示例随机密钥只在本进程有效。
不提供密钥轮换、单次使用防重放或业务授权；一次性副作用应使用持久化 nonce/幂等记录。
客户端 inputResponses 仍须用 ProtocolSchema 和业务规则校验，状态验证通过不等于输入可信。
缺少输入应继续返回 input_required，不处理多余的未请求输入。

[English](README.md)
