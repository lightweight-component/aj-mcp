package com.foo.myapp;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerSse;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Represents config.
 */
@Configuration
public class Config {
    /**
     * Executes the server sse operation.
     * @return the result of the server sse operation.
     */
    @Bean(destroyMethod = "close")
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
        serverConfig.setPageSize(8);
        server.setServerConfig(serverConfig);


        server.start();

        return serverSse;
    }
}
