# mcp-core

Maven 坐标：`com.ajaxjs:aj-mcp-next-core:1.36`（版本继承自 `ajaxjs-parent`，以 POM 为准）。
Java 8 协议基础模块，使用 AJ Util 的 `JsonEngine`，不创建线程、网络连接或全局会话。

## API 分工

| 类型                                           | 用途                                     |
|----------------------------------------------|----------------------------------------|
| `RpcRequest` / `RpcNotification`             | 请求和通知分离，通知没有 ID                        |
| `RpcSuccess` / `RpcError`                    | 结果响应和协议错误分离                            |
| `RequestId`                                  | 字符串或精确整数；数值 `1` 与字符串 `"1"` 不相等         |
| `RequestMetadata`                            | 每次请求的协议版本、能力、可选 clientInfo 和扩展 `_meta` |
| `McpCodec`                                   | JSON 和 UTF-8 字节编解码；不自动发送响应             |
| `ProtocolValidator`                          | 支持版本检查及所需能力子树检查                        |
| `McpResult` / `ToolResults`                  | `complete`、`input_required`、工具文本/结构化结果 |
| `ToolDefinition`                             | 保留完整输入/输出 Schema 和扩展字段                 |
| `SchemaValidator` / `DefaultSchemaValidator` | 离线 Schema 编译及实例校验                      |
| `ProtocolSchema`                             | 官方方法/内容 Schema、关联及逐请求语义检查              |
| `ToolContract`                               | 一次编译工具 Schema，校验调用参数及成功的结构化输出          |

所有示例类型位于 `com.ajaxjs.mcp2026.core`，Schema SPI 位于 `.schema`。
开放字段使用 Map/List/字符串/数值/布尔/null；模型构造时深拷贝为只读快照。
Bean 可先通过 AJ Util 转成 JSON 值。使用 `McpCodec` 编解码模型，不直接将模型当 Bean 序列化。

## 构造并读取工具请求

```java
import com.ajaxjs.mcp2026.core.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

McpCodec codec=new McpCodec();
        RequestMetadata metadata=RequestMetadata.create(Collections.emptyMap());
        Map<String, Object> params=new LinkedHashMap<>();
        params.put("name","search");
        params.put("arguments",Collections.singletonMap("q","Java 8"));

        RpcRequest request=RpcRequest.create(RequestId.of("call-1"),"tools/call",params,metadata);
        String json=codec.encode(request);
        RpcRequest received=(RpcRequest)codec.decode(json);
        ProtocolSchema protocol=new ProtocolSchema();
        protocol.requireRequest(received);

        RpcSuccess response=new RpcSuccess(received.getId(),ToolResults.text("Found 3 items"));
        protocol.requireResult(received,response);
        byte[]responseBytes=codec.encodeUtf8(response);
```

`create` 的 params 不允许再包含 `_meta`，避免重复来源被静默覆盖。
`clientInfo` 仅描述软件，不能用于认证。版本不匹配可以正常解码，之后
`requireSupportedVersion` 产生 `-32022` 和 `data.supported` / `data.requested`。
调用者负责在分发前执行这一检查。能力检查每次读取请求自身的元数据。

## 结果及 MRTR

```java
McpResult failedTool = ToolResults.error("Invalid business input");
McpResult structured = ToolResults.structured(
        Collections.emptyList(), java.util.Arrays.asList("a", "b"), false);
McpResult needsRetry = McpResult.inputRequired(null, "opaque-request-state");
```

工具业务失败仍使用 `RpcSuccess` 包含 `isError=true`，与 `RpcError` 区分。
`structuredContent` 可为任意 JSON 值，显式 null 不会被丢弃。
`input_required` 至少包含 `inputRequests` 或 `requestState`；后者为不透明字符串。
这里只构造/读取结果，不执行输入交互、自动重试或状态持久化。
客户端重试时可在请求 params 中携带 `inputResponses` 和 `requestState`。

## 编解码约定

- 仅按 `2026-07-28` 处理，拒绝批量报文。没有 `initialize` 或 session 状态。
- 默认复用 AJ Util 的 `Jackson2Engine`，仅覆盖入站解析策略：重复字段、尾随文档、
  非法 UTF-8 和非标准 JSON 被拒绝；小数用 BigDecimal，整数用 BigInteger。
- 每条 JSON 文档默认限制 4 MiB UTF-8 字节；可通过构造器调整。默认引擎额外限制深度 64、
  数字长度 1000、字符串长度 4 Mi 字符。模型快照也限制深度 64。
- 大小超限产生本地 `IllegalArgumentException`；解析/报文错误产生 `McpProtocolException`。
  异常的协议错误信息不携带原始输入；cause 仅供本地诊断，可能包含解析细节，勿直接暴露给对端。
- 已识别的通知处理失败时 `toErrorResponse()` 返回 null。不得序列化或发送此 null；
  HTTP 状态由未来传输模块处理。收到非法响应时由客户端报告错误，不能向对端再回复错误。
- 默认只接受 `complete` / `input_required`。扩展结果类型须显式传给 codec 构造器；
  调用方负责确认扩展已启用。缺失 `resultType` 不按旧版本自动补齐。
- 错误码使用 Java int，接收时保留未知的 32 位错误码；编码时拒绝已禁用及未分配的 MCP 保留错误码。
  无法关联的现代错误响应省略 ID。
- 自定义 `JsonEngine` 注入后固定在 codec 实例中，不受 `JsonUtil.setEngine` 影响。
  自定义引擎必须满足严格解析、精确数值和并发使用契约；codec 不会重写引擎内部策略。
- 默认引擎使用前完成配置。不要修改继承的 ObjectMapper 或在共享后修改引擎。

## 校验边界

codec 校验信封及必要元数据形状，保留方法 params、结果扩展、能力扩展和 Schema 的未知字段。
方法级校验需要显式调用，不会隐含在解码操作中。
`ToolDefinition` 检查必需的对象输入 Schema；保存 `$defs`、`oneOf`、`$ref` 等关键字，
但不会解析引用、校验参数或证明 Schema 合法。

`DefaultSchemaValidator` 使用 networknt 2.0.4（Java 8/Jackson 2），已排除 YAML。
默认使用 2020-12，额外支持显式 `http://json-schema.org/draft-07/schema#`；其他方言明确失败。
先用随库提供的元 Schema 验证 Schema 自身，再编译并提前解析引用。没有开启网络抓取的选项：
支持局部引用，未解析的网络/文件/classpath 引用失败；只允许加载随库提供的标准元 Schema。
编译计划拥有 Schema 快照，可并发复用。默认限制：深度 64、20,000 个 JSON 节点、
键/字符串/数值累计 4 Mi 字符、每个数字 1,000 字符；深度和节点数可配置。
拒绝简单的、不推进数据层级的局部指针引用环。这是结构限制，不是硬 CPU 超时或恶意正则沙箱；
正则采用引擎的 JDK 实现。`format` 遵从方言默认规则（2020-12 中仅为注解）。
校验诊断不包含被拒绝的值，只包含路径/关键字；异常 cause 仅用于本地诊断。

复用一个 `ProtocolSchema`，在分发前调用 `requireRequest`，在关联响应上调用 `requireResult`。
覆盖 10 个核心客户端方法及内置内容、通知和 MRTR 定义。客户端发来的通知使用
`requireClientNotification`；一般通知形状用 `requireNotification`；进度 token、日志阈值和
服务端取消限制用 `requireRequestNotification`。通知异常的 `toErrorResponse()` 为 null；
响应校验错误仅在本地报告，不能回发错误。未知扩展方法/结果需由扩展校验器处理，不做宽松放行。

每个注册工具构造一次 `new ToolContract(tool, new DefaultSchemaValidator(), protocol)`。
调用前执行 `requireCall(request)`，调用后执行 `requireResult(request, response)`。
省略 arguments 按空对象处理；非法参数产生 `INVALID_PARAMS`。成功输出按 `outputSchema` 校验，
显式 JSON null 与缺失 `structuredContent` 区分。工具执行失败仍使用 `isError=true`；
注册时 Schema 非法直接失败，不进入业务调用。

## 服务发现报文

`Discovery` 提供必需的 `server/discover` 方法的 core 报文支持，不创建连接、会话或缓存。

```java
RpcRequest request = Discovery.request(RequestId.of(1),
        RequestMetadata.create(Collections.emptyMap()));
McpResult result = Discovery.result(Collections.emptyMap(), "example-server", "1.0");
RpcSuccess response = new RpcSuccess(request.getId(), result);
Discovery.validate(response.getResult());
```

便捷构造器默认 `ttlMs=0`、`cacheScope=private`。如需自定义缓存提示、版本列表或
`instructions`，使用 `McpResult.complete(fields)` 后调用 `Discovery.validate`。
校验涵盖必需字段、版本数组元素、缓存范围、非负整数缓存时长及可选服务端身份的必要字段；
未知扩展字段保留。通用 codec 不推断响应对应的方法，因此客户端应按原请求方法调用校验。
客户端缓存必须按授权上下文隔离 `private` 结果；这里不实现缓存或授权策略。

core 本身不等于整个 SDK 符合协议；分发、认证、传输规则和客户端执行仍属于后续两个模块。
详见[职责清单](CORE-SCOPE.zh-CN.md)。

## 多轮后续请求

`MultiRoundTrip.retry(original, response, nextId, inputResponses)` 只构造后续请求，不发送或自动重试。
支持 `tools/call`、`resources/read`、`prompts/get`；检查响应关联、不同的请求 ID、
`input_required` 类型及本轮输入是否齐全。保留原参数和元数据，仅替换本轮输入与状态，
不解析 `requestState`，也不会把上一轮已失效的状态带入下一轮。

`RpcRequest` 对这三个方法检查 `requestState` 字符串和 `inputResponses` 对象及其对象值；
解码失败转换为带请求 ID 的 `INVALID_PARAMS`。服务端接收到部分输入时仍可继续请求补充，
因此通用解码不要求输入齐全。额外输入字段保留，由服务端忽略不需要的信息。

`ProtocolSchema.requireResult` 检查输入请求形状及逐请求能力；
`requireRetry(original, inputRequired, retry)` 进一步校验参数/状态保留、各输入对应的结果类型，
以及接受的表单内容是否符合 requestedSchema。它不是执行引擎：超时、取消、轮数上限、
状态完整性验证及重放保护仍由 client/server 实现。客户端原样回传状态，服务端不能信任它。
服务端收到部分输入时仍可再次请求补充。

## 订阅报文

`Subscriptions.listen(id, metadata, filter)` 构造订阅请求；`RpcRequest` 自动检查该方法
必需的 `notifications` 对象及内置布尔开关、资源字符串数组。空过滤器合法，未知扩展保留。
`Subscriptions.acknowledge(request, accepted)` 构造首条确认通知，检查接受的内置类型和资源
是请求范围的子集；扩展过滤器的协商与语义由扩展实现负责。
`notificationId(notification)` 读取并校验订阅关联 ID，确认通知同时检查过滤器形状。
`closed(request)` 构造正常结束响应，`validateClosed(request, response)` 校验类型及双重 ID 关联。

`ProtocolSchema.requireSubscriptionNotification` 检查接受的内置通知类型及关联 ID。
资源 URI 默认精确匹配；谓词重载允许资源提供者定义合法子资源关系，不猜测 URI 前缀。
确认是通知，不是完成响应；完成响应只在正常结束时发送。发送顺序、能力支持、
取消、流关闭和重连仍由 client/server 实现。
通用 codec 不自动识别订阅响应或流上通知，调用方须使用上述关联检查。

## 构建与测试

共享纯辅助类 HttpMetadata 和 WireIO 提供头/正文绑定、x-mcp-header 校验、安全头编码和有界 UTF-8 帧读写。
它们不打开连接、不创建线程、不持有流所有权；实际传输生命周期和执行仍属于 client/server。

先按[工程说明](../README.zh-CN.md)安装本地重构版 AJ Util，再从仓库根目录执行：

```sh
mvn -pl aj-mcp-2026/mcp-core -am -DskipTests=false test
mvn -f aj-mcp-2026/pom.xml -DskipTests=false package
```

测试覆盖真实 AJ Util 引擎、JSON 边界、ID 精度、逐请求隔离、只读快照、通知错误抑制、
扩展结果策略、方法 Schema、工具契约、离线引用、MRTR 关联/能力、订阅过滤及并发校验。

[English](README.md)
