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

创建客户端后，在与服务器通信前，需要先进行初始化。也就是说，在发起任何请求之前，必须先调用 `initialize()` 方法初始化协议。  
`initialize();` 方法会初始化传输通道并设置通知处理。

Initialize 方法的步骤如下：

1. 向服务器发送初始化请求
2. 保存服务器的能力信息
3. 发送 “initialized” 通知
4. 标记客户端为已初始化状态

初始化成功后，客户端才能向服务器发起其他请求。

因此，在创建完 McpClient 后，应立即调用 `initialize();` 方法。

```java
McpClient mcpClient = McpClient.builder()
        .clientName("my-host")
        .clientVersion("1.2")
        .transport(sseTransport)
        .build();

mcpClient.initialize();
```