---
title: AJ MCP Next
description: 面向逐请求现代 MCP 的 Java 8 SDK
layout: layouts/docs-cn.njk
---

# AJ MCP Next

独立的 aj-mcp-2026 工程仅面向 MCP 2026-07-28。Maven 制品为
com.ajaxjs:aj-mcp-next-core、aj-mcp-next-client、aj-mcp-next-server，版本继承 ajaxjs-parent。
不沿用旧 SDK 的 initialize 握手和 HTTP 会话。

Core 提供消息、严格 JSON/Schema 校验和协议辅助；client 提供有界调用、MRTR 输入 handler、订阅及 STDIO/HTTP；
server 提供注册分发、生命周期、AJ Util AES-GCM 状态保护和 STDIO/JDK HTTP。
复用 AJ Util JSON 引擎及 AJ HTTP 连接初始化。

默认调用超时 30 秒。HTTP 默认本地回环、拒绝未允许的 Origin，各请求独立 POST 响应流。
远程部署仍需认证、TLS 和宿主网络限制。不内置自动工具重放、旧协议回退、共享授权缓存、OAuth 授权服务或 Tasks 扩展。

完整示例和限制位于仓库文档：
[工程](https://github.com/lightweight-component/aj-mcp/tree/main/aj-mcp-2026)、
[client](https://github.com/lightweight-component/aj-mcp/tree/main/aj-mcp-2026/mcp-client)、
[server](https://github.com/lightweight-component/aj-mcp/tree/main/aj-mcp-2026/mcp-server)。
本地源码需提交/发布后才会出现在托管仓库中。

在仓库根目录执行 `mvn -f aj-mcp-2026/pom.xml -DskipTests=false clean verify`。
verify 覆盖真实 HTTP、子进程 STDIO 和打包 JAR 示例，不代表官方符合性认证。
