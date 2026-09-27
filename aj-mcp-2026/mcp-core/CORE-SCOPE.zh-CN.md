# aj-mcp-next-core 职责与验证清单

本轮完成的是三个模块设计中的 **core：报文、编解码、结构校验和无状态协议辅助操作**。
它不是一个可直接联网运行的客户端/服务端，不以 core 的测试通过宣称整个 SDK 已完成协议符合性认证。

| 协议职责                    | core 提供                                                | 接入要求 / 后续模块责任                             |
|-------------------------|--------------------------------------------------------|-------------------------------------------|
| JSON-RPC 信封、UTF-8、通知无响应 | `McpCodec`、不可变模型、通知异常抑制                                | 传输层不能写出 null 响应；STDIO stdout 只承载协议        |
| HTTP 元数据 / 帧辅助          | HttpMetadata、WireIO：纯头绑定、有限 UTF-8 流读写                  | client/server 打开、管理并关闭实际连接和流              |
| 逐请求版本与能力                | `RequestMetadata`、`ProtocolValidator`、`ProtocolSchema` | 每次调用检查，不在连接上缓存协商上下文                       |
| 错误码                     | 标准错误、未知入站错误保留、禁止发送废弃/未分配保留码                            | 分发层映射业务异常；调用者不向非法响应回发错误                   |
| 10 个核心客户端方法             | 固定官方 Schema 的方法级请求/结果检查                                | 注册与执行 tools/resources/prompts 等能力，不虚报支持   |
| Schema                  | 2020-12、显式 draft-07、元 Schema 校验、离线引用和结构限制              | 注册时编译并复用；其他方言明确拒绝，扩展可注入校验器                |
| 工具入参和输出                 | `ToolContract`                                         | 业务授权、限流和副作用仍由服务端负责                        |
| 发现与缓存字段                 | Discovery、非负整数 TTL、public/private 形状                   | 缓存过期与授权上下文隔离由 client 管理                   |
| MRTR                    | input_required、后续请求构造、输入类型/能力/表单检查                     | client 执行输入交互、限制轮次/时长；server 验签状态、防重放     |
| 订阅                      | listen/ack/close、ID/过滤器与已接受通知范围校验                      | 每个流先 ack；注册、派发、取消、关闭和重连由 client/server 管理 |
| 进度、日志、取消                | 官方通知 Schema、进度 token、日志阈值、服务端取消方法限制                    | 请求是否仍在途、进度是否递增、STDIO/HTTP 差异由运行时管理        |
| 可选扩展                    | 保留开放字段；codec 显式配置结果类型                                  | 扩展实现自己的能力协商与语义校验，不调用严格 core 路由冒充支持        |

## 接入顺序

1. 使用 codec 解码。它只校验信封，不隐式初始化 Schema 引擎。
2. core 方法请求调用 `protocol.requireRequest`；工具注册时创建 ToolContract，调用前执行其 `requireCall`。
3. 返回结果调用 `protocol.requireResult` 或工具契约的 `requireResult`，然后编码。
4. 客户端关联到原请求后同样校验响应。通知使用方向/关联适用的检查入口，不生成响应。
5. MRTR 后续请求用 `MultiRoundTrip.retry` 构造、`protocol.requireRetry` 检查；订阅流调用对应过滤/关联检查。

## 验证范围与限制

- 在实际 JDK 8 和 JDK 17 上运行 core 测试与构建，另验证根仓库按模块选择构建。
- 测试包括离线拒绝引用、合法局部递归、简单无限递归拒绝、Schema/实例错误、工具契约、MRTR、订阅及并发。
- Schema 引擎为 networknt 2.0.4，新增的生产依赖另有 `itu` 日期格式库；没有 YAML/HTTP/Servlet/Spring。
- 结构限制不是恶意 Schema 的硬实时隔离；复杂正则和组合 Schema 的 CPU 成本仍需宿主限制。
  默认 `format` 不作为 2020-12 断言。未宣称通过整个官方 JSON Schema 测试套件。
- 仅面向 2026-07-28；旧版本协商/兼容适配、扩展实现以及传输符合性测试不属于当前 core。

## 官方 Schema 快照

运行时只读取 JAR 内资源，不访问官网或跟随 main 自动更新。
来源、获取日期、文件摘要及许可证见 JAR 内 `META-INF/MCP-SCHEMA-NOTICE.txt` 和 `MCP-SCHEMA-LICENSE.txt`。
升级协议时应显式更新资源、核对文本约束并运行测试，不能仅替换版本常量。

[English](CORE-SCOPE.md)
