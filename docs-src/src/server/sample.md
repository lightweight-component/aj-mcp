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

The repository contains two integration samples: a standalone Tomcat server and a Spring Boot application, each exposing a simple MCP service.

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
        connector.setProperty("connectionTimeout", "60000"); // 60 seconds
        connector.setProperty("keepAliveTimeout", "60000"); // 60 seconds
        connector.setProperty("maxKeepAliveRequests", "100"); // Optional: Max requests per connection

        tomcat.start();
        tomcat.getServer().await();
    }
}
```

## Spring Application Integration

The Spring configuration uses the same setup pattern, with `ServerSse` managed by the Spring container.

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

## Design Note

The legacy HTTP/SSE transport uses two endpoints:

- **SSE URL**: the client opens this connection first. The server registers the session with `openSession(clientId, writer, postPath)` and sends an `endpoint` event containing the POST path.
- **POST URL**: the client sends JSON-RPC requests to this endpoint. The controller calls `serverSse.handle(clientId, json)`, and the response is written back only to the originating SSE session—not to the POST response and not to every connected client.

`ServerSse.start()` starts one shared heartbeat scheduler. Do not create a heartbeat thread per HTTP request. Remove the session when the request ends, and close `ServerSse` when the application shuts down.
