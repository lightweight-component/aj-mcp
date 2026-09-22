package com.foo.streamable;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.foo.streamable.features.DemoTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class McpConfiguration {
    @Bean public RequestContext requestContext() { return new RequestContext(); }

    @Bean
    public McpServer mcpServer(RequestContext context, @Value("${sample.allowed-origins:}") String origins) {
        McpServer server = new McpServer();
        ServerConfig config = new ServerConfig();
        config.setName("streamable-http-demo"); config.setVersion("1.0"); config.setPageSize(20);
        config.setClientRequestTimeout(Duration.ofSeconds(5));
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        server.setServerConfig(config);
        server.getFeatureMgr().init("com.foo.streamable.features");
        // Annotation scanning is framework-neutral and does not perform Spring injection.
        server.getFeatureMgr().getToolStore().values().forEach(store -> {
            if (store.getInstance() instanceof DemoTools)
                ((DemoTools) store.getInstance()).configure(server, context);
        });
        return server;
    }

    @Bean(destroyMethod = "close")
    public ServerStreamableHttp mcpTransport(McpServer server) {
        ServerStreamableHttp transport = new ServerStreamableHttp(server);
        server.setTransport(transport);
        transport.start();

        return transport;
    }
}
