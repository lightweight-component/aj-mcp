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
        connector.setProperty("connectionTimeout", "60000"); // 20 seconds
        connector.setProperty("keepAliveTimeout", "60000"); // 30 seconds
        connector.setProperty("maxKeepAliveRequests", "100"); // Optional: Max requests per connection

        tomcat.start();
        tomcat.getServer().await();
    }
}
```


## Spring 应用集成

Spring 配置演示了依赖注入的设置方式，见 `Config.java:12-29`，其中 ServerSse Bean 按相同模式配置，但由 Spring 容器管理。


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

对于基于 SSE 的 MCP 服务器，有两个端点需要了解：

- **SSE Url**：这是客户端最初连接的端点，在初始化时使用。在此初始化过程中，服务器会返回一个 POST Url（第二个端点）。
- **POST Url**：这是 MCP 业务的实际端点，客户端将请求发送到该端点，服务器也会通过该端点返回响应。它同样是一个 SSE 端点。