---
title: MCP Server SDK Integration Samples
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK Integration Samples
date: 2022-01-05
tags:
  - Integration
  - Samples
layout: layouts/docs.njk
---

# MCP Server SDK Integration Samples

The source code repository of this project contains two integration samples: one is a standalone Tomcat server with a simple MCP service,
and the other is a Spring Boot application with a MCP service.

## Tomcat Application Integration

The Tomcat deployment shows a complete server setup pattern:

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

## Spring Application Integration

The Spring configuration demonstrates dependency injection setup `Config.java:12-29`, where the ServerSse bean is configured with the same pattern but
managed by Spring's container.

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

## Design Note

For Mcp server over SSE, there are two endpoints that should be known:

    SSE Url, this is the endpoint that the client will first connect to, which is at time of initialization. In this initialization, it'll return a POST Url(The second endpoint) form the server.
    POST Url, this is the real endpoint for the MCP business, client will send the request to this endpoint and the server will return the response by this endpoint. It's an SSE endpoint.
