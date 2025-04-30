package com.foo.myproduct;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerStdio;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.model.ServerConfig;

public class App {
    public static void main(String[] args) {
        FeatureMgr mgr = new FeatureMgr();
        mgr.init("com.foo.myproduct");

        McpServer server = new McpServer();
        server.setTransport(new ServerStdio(server));
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("MY_MCP_Server");
        serverConfig.setVersion("1.0");
        server.setServerConfig(serverConfig);

        server.start();
    }
}
