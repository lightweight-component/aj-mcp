---
title: Initializing the Protocol
subTitle: 2024-12-05 by Frank Cheung
description: Initializing the Protocol
date: 2022-01-05
tags:
  - initialization
layout: layouts/docs-cn.njk
---

# 协议初始化

## Streamable HTTP 会话过期

带会话 ID 的 POST 或 GET 收到 HTTP 404 后，传输层会去掉旧会话 ID，重新执行初始化握手。
旧的待处理调用会异常完成，**不会自动重放**，避免有副作用的工具被重复执行。
新调用等待重建完成，可通过 `transport.getSessionRecovery()` 观察成功或失败。
重建会清理旧 GET 连接和事件游标，并在启用 GET 时重新连接；重建失败不会无限重试。
如果服务端选择了不同的协议版本，需要显式创建新客户端。资源订阅等应用状态需要由应用在恢复后重新建立。

创建客户端后，应在发送其他协议请求前调用 `initialize()`。该方法会启动传输层、协商协议版本和能力，并发送
`notifications/initialized` 通知。

Initialize 方法的步骤如下：

1. 向服务器发送初始化请求
2. 记录服务器返回的初始化结果
3. 发送 “initialized” 通知

初始化成功后，客户端才能向服务器发起其他请求。

服务端会在协议协商前校验 initialize 报文：`params` 和 `capabilities` 必须是 JSON object；`protocolVersion`、
`clientInfo.name` 与 `clientInfo.version` 必须是非空字符串。客户端输入不合法时返回 JSON-RPC `INVALID_PARAMS`，而不是
internal error。

客户端能力必须在调用 `initialize()` 前注册。例如，应先调用 `setRoots(...)`、`setSamplingHandler(...)` 或
`setElicitationHandler(...)`；只有已注册 handler 的能力才会被声明。服务端对未声明 Roots 或 Sampling 能力的 session
发起相应请求时，会在本地直接拒绝。

因此，构建 `McpClient` 后应立即调用 `initialize()`。初始化遵循 `requestTimeout` 配置；设置为 0 时表示无限等待。

```java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();

mcpClient.initialize();
```
