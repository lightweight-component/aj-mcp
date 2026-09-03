---
title: MCP Server SDK 整合演示
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK 整合演示
date: 2022-01-05
tags:
  - MCP Server SDK 整合演示
layout: layouts/docs-cn.njk
---

# MCP 服务器 SDK 集成示例

本项目的源代码仓库包含两个集成示例：一个是带有简单 MCP 服务的独立 Tomcat 服务器，另一个是带有 MCP 服务的 Spring Boot 应用程序。

## Tomcat 应用集成

Tomcat 部署展示了一个完整的服务器搭建模式：

```java
package com.foo.myapp;


import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerSse;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class StandaloneTomcat {
    public static void main(String[] args) throws Exception {
        FeatureMgr mgr = new FeatureMgr();
        mgr.init("com.foo.myapp");

        McpServer server = new McpServer();
        server.setFeatureMgr(mgr);
        ServerSse serverSse = new ServerSse(server);
        server.setTransport(serverSse);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("MY_MCP_Server");
        serverConfig.setVersion("1.0");
        server.setServerConfig(serverConfig);

        server.start();

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);

        // Set base directory (for temp files)
        tomcat.setBaseDir(".");

        // Create a context (no web.xml required)
        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);

        // Register SSE servlet
        SseServlet sseServlet = new SseServlet(serverSse);
        Tomcat.addServlet(context, "sseServlet", sseServlet);
        context.addServletMappingDecoded("/sse", "sseServlet");

        // Register Message servlet
        Tomcat.addServlet(context, "messageServlet", new MessageServlet(serverSse));
        context.addServletMappingDecoded("/message", "messageServlet");

        // Configure connectionTimeout and keepAliveTimeout
        Connector connector = tomcat.getConnector();
        connector.setProperty("connectionTimeout", "60000"); // 60 秒
        connector.setProperty("keepAliveTimeout", "60000"); // 60 秒
        connector.setProperty("maxKeepAliveRequests", "100"); // Optional: Max requests per connection

        tomcat.start();
        tomcat.getServer().await();
    }
}
```

## Spring 应用集成

Spring 配置采用相同的初始化方式，并由 Spring 容器管理 `ServerSse` Bean。

```java
package com.foo.myapp;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerSse;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public ServerSse serverSse() {
        FeatureMgr mgr = new FeatureMgr();
        mgr.init("com.foo.myapp");

        McpServer server = new McpServer();
        server.setFeatureMgr(mgr);
        ServerSse serverSse = new ServerSse(server);
        server.setTransport(serverSse);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("MY_MCP_Server");
        serverConfig.setVersion("1.0");
        server.setServerConfig(serverConfig);

        server.start();

        return serverSse;
    }
}
```

## 设计说明

旧版 HTTP/SSE 传输包含两个端点：

- **SSE URL**：客户端首先建立该连接。服务端通过 `openSession(clientId, writer, postPath)` 注册会话，并发送包含 POST 路径的
  `endpoint` 事件。
- **POST URL**：客户端把 JSON-RPC 请求发送到该端点。控制器调用 `serverSse.handle(clientId, json)`；响应只写回发起请求的 SSE
  会话，不写入 POST 响应，也不会广播给其他客户端。

`ServerSse.start()` 会启动一个共享的心跳调度器，不要为每个 HTTP 请求单独创建心跳线程。请求结束时应移除对应会话，应用关闭时应关闭
`ServerSse`。
