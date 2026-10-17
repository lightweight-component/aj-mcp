package com.ajaxjs.mcp2026.sample.server;

import com.ajaxjs.mcp2026.core.ToolDefinition;
import com.ajaxjs.mcp2026.core.ToolResults;
import com.ajaxjs.mcp2026.server.HttpServerTransport;
import com.ajaxjs.mcp2026.server.McpServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Spring Boot entry point hosting two small MCP tools over the SDK HTTP adapter. */
@SpringBootApplication
public class SampleServerApplication {
    /**
     * Starts the application and registers its shutdown hook.
     * @param args Spring Boot command-line properties
     */
    public static void main(String[] args) {
        SpringApplication.run(SampleServerApplication.class, args);
    }

    /**
     * Registers tools whose handlers can be replaced with calls to application services.
     * @return server owned by the Spring application context
     */
    @Bean(destroyMethod = "close")
    public McpServer mcpServer() {
        McpServer server = new McpServer("tutorial-server", "1.0");
        try {
            server.tool(ToolDefinition.create("echo", stringInput("text", "需要返回的文字"))
                    .with("description", "原样返回收到的文字，用于检查工具调用是否正常"), context -> {
                Map<?, ?> arguments = (Map<?, ?>) context.request().getParams().get("arguments");
                return ToolResults.text((String) arguments.get("text"));
            });
            server.tool(ToolDefinition.create("query_order", stringInput("orderId", "订单号，例如 A1001"))
                    .with("description", "根据订单号查询模拟订单的发货状态"), context -> {
                Map<?, ?> arguments = (Map<?, ?>) context.request().getParams().get("arguments");
                String orderId = (String) arguments.get("orderId");
                if ("A1001".equals(orderId))
                    return ToolResults.text("订单 A1001 已发货，承运方：示例快递，运单号：DEMO1001。");
                return ToolResults.error("没有找到订单 " + orderId + "，请检查订单号。示例订单号为 A1001。");
            });
            return server;
        } catch (RuntimeException ex) {
            server.close();
            throw ex;
        }
    }

    /**
     * Starts the loopback MCP endpoint after tool registration; Spring closes it before the server.
     * @param server registered tool dispatcher
     * @param port configurable MCP port, with zero selecting an available port
     * @return running transport
     * @throws IOException if the listening address cannot be bound
     */
    @Bean(initMethod = "start", destroyMethod = "close")
    public HttpServerTransport mcpHttpTransport(McpServer server,
                                               @Value("${mcp.port:8080}") int port) throws IOException {
        return new HttpServerTransport(server, port);
    }

    /**
     * Describes one required string argument using Java 8 collection APIs.
     * @param name argument name
     * @param description argument explanation exposed to callers
     * @return object input schema
     */
    private static Map<String, Object> stringInput(String name, String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "string");
        property.put("description", description);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections.singletonMap(name, property));
        schema.put("required", Collections.singletonList(name));
        schema.put("additionalProperties", false);
        return schema;
    }
}
