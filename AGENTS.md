# AJ-MCP 项目协作约定

## 适用范围

本文件适用于整个仓库，供参与开发、审查、测试和文档维护的 AI 助手使用。
若子目录新增 `AGENTS.md`，其细化规则在对应目录内适用；用户明确要求优先于本文件。

涉及 SDK、协议、传输、示例或文档时，先阅读
[aj-mcp-java8 skill](.agents/skills/aj-mcp-java8/SKILL.md)。本文件规定全局约束，skill 提供具体工作流程；
修改约定时应检查两者一致性，避免重复维护相互矛盾的规则。

## 项目目标与目录

AJ-MCP 为旧 Java 8 系统提供 MCP 客户端和服务端能力。保持轻量、可嵌入和兼容性，不以升级 JDK 解决问题。

| 目录 | 职责 |
|---|---|
| `aj-mcp-common` | JSON-RPC/MCP 模型、协议版本、共享工具 |
| `aj-mcp-client` | 客户端 API、缓存、请求管理及各类传输 |
| `aj-mcp-server` | 协议分发、注解注册、功能调用及服务端传输 |
| `samples/client` | 通用客户端与 `StreamableHttpClientExample` |
| `samples/server/server-stdio` | STDIO 服务端示例 |
| `samples/server/spring`、`samples/server/tomcat` | 旧 HTTP/SSE 示例 |
| `samples/server/spring-streamable-http` | Spring Boot Streamable HTTP 示例 |
| `docs-src` | 文档源文件和构建配置 |
| `to-fix.md` | 问题清单与历史修复记录，不是当前代码正确性的证明 |

根 `pom.xml` 是聚合构建入口；不要假设各模块都继承它的配置。
依赖、版本、插件和模块信息以实际 POM 为准，不在协作规则中固定发布版本号。

## 开始工作前

- 查看 `git status`，保留用户已有修改；不要清理、覆盖或回滚无关文件。
- 优先使用 `rg`、`rg --files` 定位代码，追踪模型、API、分发和传输的完整调用链。
- 审计或大范围修复前阅读 `to-fix.md`，并重新对照源码确认，不照抄历史结论。
- “分析、审查、给方案”不等于授权修改；用户要求实施后再落实代码变更。
- 默认用中文沟通。新增关键代码注释使用英文，解释协议约束、并发边界和设计原因，避免空泛注释。
- 不擅自提交、推送、发布制品或修改无关版本；不为局部修复顺带做大规模重构。

## 兼容性和协议边界

- 生产代码保持 Java 8 语法和 JDK API 兼容，禁止引入 `var`、record、`List.of` 等新版本特性。
- 不静默升级 JDK、Spring 或依赖基线；确有需要时先说明原因及影响。
- 保持现有公共 API；优先新增重载或可选配置，不随意改变默认行为。
- 当前协议版本为 `2024-11-05`、`2025-03-26`、`2025-06-18`，通过 `ProtocolVersion` 集中管理能力差异。
- 初始化协商版本后再使用相应能力；正向和反向请求都检查版本及 capability。
- JSON-RPC batch 按项目范围不实现，除非用户明确改变要求；不能因此宣称完全符合所有版本的规范。
- 传输自动识别与协议版本协商是两件事；回退旧 HTTP/SSE 不等于强制选择旧协议版本。
- 判断协议行为时查阅对应版本的官方规范，区分必选行为、可选能力与项目主动不支持的内容。

## 必须保持的正确性

- notification 不产生 JSON-RPC 响应，包括处理失败的情况；传输层不序列化或写出 null response。
- 工具始终提供非 null 的对象 `inputSchema`；允许省略可选参数，保留合法 JSON Schema 信息。
- 未知功能、非法参数和业务异常返回可识别的协议错误；保留异常 cause，库代码不直接 `printStackTrace()`。
- STDIO 的 stdout 仅用于协议输出，使用 UTF-8；日志不能混入 stdout。
- 请求 ID、取消、订阅、日志阈值等状态按连接或会话隔离；功能注册表保持实例级。
- 请求必须有明确的等待边界；null/零超时按现有有限默认值处理，负数拒绝。
- 捕获 `InterruptedException` 后恢复中断标志，再传播或转换异常。
- 传输关闭、进程退出或会话失效时，相关 pending futures 应异常完成，不留下永久等待。
- Streamable HTTP 可选 GET 断开不应直接终止无关 POST；会话 404 重建不自动重放可能有副作用的操作。
- 仅在首次初始化的适用端点错误上回退旧 HTTP/SSE，不把认证、限流、超时或业务错误当作回退信号。
- HTTP 响应、EventSource、线程池、流、子进程和会话都要有幂等关闭路径；初始化失败也必须清理。
- 同一 STDIO/SSE 输出串行写入；使用 `PrintWriter` 时检查 `checkError()`。
- GET 使用框架异步生命周期，完成、错误和超时按 writer 身份清理，不能误关替换后的连接。
- 包扫描不触发静态初始化，单个不可加载的可选类不应使整个扫描失败。
- 校验 Origin 与 endpoint 边界，避免凭据转发到非预期来源；Origin 白名单不等于认证或 CORS。

## 测试与验证

修复应在所属模块添加回归测试。并发测试优先使用 latch、barrier 和有界等待，不依赖大量 sleep 或概率重试。
跨传输边界的改动覆盖正常、失败、关闭、notification 和会话隔离路径。
HTTP 示例优先用随机端口、真实嵌入式服务器及 SDK 客户端，不依赖外部网络服务。

从根目录执行，先针对性验证，再扩大范围；显式使用 `-DskipTests=false` 避免继承配置跳过测试：

```sh
# 将 TestClassName 替换为实际测试类
mvn -pl aj-mcp-client -am -DskipTests=false -Dtest=TestClassName -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl aj-mcp-server -am -DskipTests=false test

# 新示例的真实 HTTP 与生命周期测试
mvn -pl samples/server/spring-streamable-http -am -DskipTests=false -Dtest=StreamableHttpSampleTest,McpControllerLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false test

# 全仓库编译和测试是不同验证层级
mvn -DskipTests compile
mvn -DskipTests=false test
```

- 使用本机已安装、兼容构建工具的 JDK，通常为 JDK 8 或 17；不要把机器专属 JDK 绝对路径写进项目配置。
- 使用 JDK 17 构建成功不等于已经验证 Java 8 运行时；报告时说明实际 JDK 和验证范围。
- 部分历史客户端集成测试需要独立服务、外部 API 或特定示例 JAR。区分环境缺失、既有失败与本次回归，
  不删除断言或静默跳过失败来制造“全绿”。筛选测试时说明筛选范围。
- 运行 sample 前先构建本地依赖，避免加载同版本号但内容较旧的已发布制品。
- 新模块加入根 POM；新增可执行示例除编译外，还应实际运行入口及打包产物，并关闭测试进程。

## 文档与交付

- `docs-src` 中 `*-cn.md` 为中文，对应不带 `-cn` 的文件为英文；首页为 `cn.md` / `index.md`。
- README 使用项目已有的 `README.md` / `README.zh-CN.md` 配对。
- 公共 API、默认值、配置、生命周期、协议支持或示例用法变化时，同步两种语言和相关示例。
- 保留仍有效的旧内容；明确区分旧 HTTP/SSE、新 Streamable HTTP 及自动识别的用法。
- 文档变更后执行 `cd docs-src && npx @11ty/eleventy`。不要把生成目录当源文件，提交生成物前先确认仓库惯例。
- 交付前检查 `git diff --check` 和变更范围，说明完成项、实际测试结果及未验证部分。
- 不以剩余问题清单或旧测试结果代替本次验证，不把可选功能未实现误报为必然 bug。
