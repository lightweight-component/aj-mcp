# AJ MCP Next Client

兼容 Java 8，仅面向现代 MCP `2026-07-28`。依赖 `aj-mcp-next-core` 和重构后的
`com.ajaxjs:aj-net:2.1`，不引入 Spring、OkHttp 或旧 MCP SDK。

## HTTP

```java
try(McpClient client=new McpClient(new HttpClientTransport("http://127.0.0.1:8080/mcp"))){
        McpResult discovery=client.discover();
        List<ToolDefinition> tools=client.listTools();
        McpResult result=client.callTool("echo",Collections.singletonMap("text","hello"));
        }
```

导入 `com.ajaxjs.mcp2026.client.*`、`com.ajaxjs.mcp2026.core.*`、`java.util.*`。
client 拥有 transport；不同端点/授权身份应使用不同 client。HTTP 构造器可传固定请求头、I/O 超时和并发数。
默认 TLS 证书及主机名检查保持开启；拒绝重定向，不会把 Authorization 转发到新端点。
复用 AJ HTTP 的连接初始化，不调用会记录请求体的高层 `connect()`。

每次调用独立 POST，支持 JSON 和请求级 SSE 响应，包括注释、多行 data。
不使用会话 ID、GET 流、恢复游标、初始化握手或自动重放。
自动镜像标准 HTTP 头及嵌套属性的 `x-mcp-header`，安全编码非 ASCII 等值。
HTTP `tools/list` 结果会剔除头注解非法的工具。

HTTP 的 `callTool` 会先刷新工具描述（包含所有分页）。
低层 `call`、`callAsync`、`exchange`、`callWithInputs` 调用 `tools/call` 前，
先执行 `listTools()`，或向 `HttpClientTransport.tool()` 显式注册可信描述。
描述仅用于请求头绑定，不代表缓存仍新鲜或授权仍有效；变更后请刷新。
HeaderMismatch 会交给调用者，不自动重试。

## STDIO

```java
try(McpClient client=new McpClient(StdioClientTransport.launch(
        Arrays.asList("java","-cp",runtimeClasspath,"com.ajaxjs.mcp2026.server.example.EchoServer")))){
        McpResult result=client.callTool("echo",Collections.singletonMap("text","hello"));
        }
```

子进程 stderr 单独继承，不混入 stdout；UTF-8 换行消息串行写出。
关闭时先尝试 stdin EOF，再进行有界终止/强制终止。传入自定义流时，流必须支持 close 解除并发 I/O 阻塞。
宿主日志必须输出到 stderr，不能污染协议 stdout。

## 调用及限制

- `call(method, params, timeoutMs)`：同步结果；线程中断时取消请求并恢复中断标志。
- `callAsync(...)`：结果 future；取消它会取消实际请求。
- `request(...)` / `exchange(request, timeoutMs)`：低层关联响应 API。
- `listAll(method, field, maxPages)`：有界分页，检测重复游标。
- 超时 0 使用 30 秒默认值，负数拒绝；关闭或传输失败会使 pending 请求异常完成。
- `McpRemoteException` 保留远端 `RpcError`；同步失败外包 `CompletionException`。
- HTTP 默认连接/读取空闲超时 15 秒，16 个 I/O worker、128 个排队任务。请求总超时包含排队时间。
  取消 HTTP 调用仅断开对应流，不影响其他 POST。
- 通知回调和同步 future continuation 运行在接收/完成线程，不应阻塞；
  耗时操作使用自己的线程池或 `thenApplyAsync`。
- 自定义 metadata 中的 progressToken 应在在途请求间唯一。
- 消息、行和 SSE 事件上限为 4 MiB，另有解析深度限制。

## MRTR 与订阅

只声明实际支持的客户端能力：

```java
RequestMetadata metadata=RequestMetadata.create(
        Collections.singletonMap("roots",Collections.emptyMap()));
        try(McpClient client=new McpClient(transport,metadata,notification->{})){
        client.listTools(); // Required for low-level HTTP tool calls.
        CompletableFuture<McpResult> result=client.callWithInputs(
        "tools/call",Collections.singletonMap("name","roots"),30_000,8,
        inputRequired->Collections.singletonMap("r",
        Collections.singletonMap("roots",Collections.emptyList())));
        }
```

另导入 `java.util.concurrent.CompletableFuture`。输入 handler 的 key 必须对应实际请求的 input ID，
示例假设服务器请求 `r`。SDK 校验 roots/sampling/elicitation 结果和接受的表单数据，
不会自动同意对话框、访问本地文件或执行 LLM 调用。
总超时覆盖输入 handler，maxRounds 限制总尝试次数；每轮使用新 ID、原样回传不透明状态。
输入 handler 应响应中断，Java 无法强制停止任意业务代码。

`subscribe(filter, lifetimeMs, updates)` 返回带 `acknowledged()`、`completion()`、`close()` 的 Subscription。
先 ack 后通知，并校验 ID 与接受的过滤器；lifetime 为 0 使用有限的 30 秒默认值，长订阅应显式指定。
订阅结束/断线后是否重新订阅由应用决定，SDK 不会因此重放工具调用。

[English](README.md)
