# 本轮实现记录与待确认取舍

本轮按“Java 8、三个模块、轻量、复用 AJ Util”先行实现，未等待人工确认。
这里列出已采用的默认方案，不是阻塞清单。

| 事项       | 已采用方案                                                      | 以后如需调整                         |
|----------|------------------------------------------------------------|--------------------------------|
| Maven 命名 | aj-mcp-next-core/client/server，沿用 ajaxjs-parent:1.36 的版本继承 | 是否给 Next 独立发布版本；未擅改目录/包名       |
| 传输       | client/server 内置 STDIO 与无会话 Streamable HTTP                | Servlet/Spring 可另写薄适配器，不另拆基础模块 |
| AJ Util  | JSON 引擎、AJ HTTP 初始化、AES-GCM                                | 本地同版本旧制品必须从当前 aj-util 源码重建     |
| 服务注册     | 显式 tool/resource/prompt/method，避免包扫描/反射依赖                  | 注解只是可选语法糖，暂不迁移旧扫描器             |
| 缓存       | 默认 TTL 0/private，不共享授权缓存；callTool 先刷新描述                    | 高频场景以后增加显式、按授权身份隔离的缓存          |
| MRTR     | 有界自动续轮；输入由显式 handler 决定                                    | 业务接入 roots、LLM、用户确认 UI，不自动同意   |
| 状态安全     | AJ AES-GCM、到期、参数/身份绑定                                      | 部署密钥管理、轮换和一次性防重放由业务补充          |
| 订阅       | 请求级、有限生命周期、精确 URI 匹配、显式重订阅                                 | 子资源关系需 provider 定义，不猜测 URI 前缀  |
| HTTP 认证  | 可插入 Authentication，默认仅本地无认证示例                              | 远程必须接认证/TLS；未内置 OAuth 授权服务     |
| 旧协议/扩展   | 不自动回退旧协议，不实现 Tasks/任意扩展执行                                  | 与旧 SDK 并存，不宣称全版本支持             |

## 实际边界

- 内置 JDK HTTP 是阻塞式轻量 adapter，不是高并发公网网关；活动 SSE 占用 HTTP worker。
  网络读写/请求头超时、TLS、限流及业务授权仍需宿主管理。
- Java 中断属于协作取消；忽略中断的业务 handler 无法被 SDK 安全强杀。
  回调需快速返回；同步 future continuation 不应阻塞接收/超时线程。
- RequestState 不代替输入校验、权限检查和单次执行账本。
- 低层 HTTP tools/call 前需 listTools 或显式注入工具描述，以满足自定义头镜像；普通 callTool 自动刷新。
- Schema 引擎仍使用 core 的固定官方快照；测试成功不等于官方符合性认证。
- aj-net:2.1 本机缓存曾缺少重构后的 Request，已从相邻 aj-util 源码仅构建/install 到本机；
  该准备步骤跳过 AJ HTTP 自身测试，未修改其源码或发布远端。

## 验证入口

本轮最终结果：实际 JDK 8u181 的独立 clean verify、JDK 17.0.2 的根仓库选模块 verify 均通过。
每轮 119 项测试（core 85、client 8、server 25、打包集成 1），失败/错误/跳过均为 0。
Eleventy 文档构建、git diff --check 均通过；测试 EchoServer 子进程已退出。
未运行整个旧 SDK 的外部服务集成测试，未提交、推送或发布制品。

```sh
mvn -f aj-mcp-2026/pom.xml -DskipTests=false clean verify
mvn -pl aj-mcp-2026/mcp-server -am -DskipTests=false verify
```

verify 包含真实随机端口 HTTP、子进程 STDIO、打包后 JAR 示例、UTF-8、头镜像、Origin、
认证、MRTR、订阅、错误、超时/取消、关闭、请求隔离和状态篡改等验证。
只运行 package 会跳过打包后的 Failsafe 集成测试。旧工程依赖外部服务的集成测试不属于本轮验证。

[English](IMPLEMENTATION-NOTES.md)
