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