package com.ajaxjs.mcp.client.gitee;

import com.ajaxjs.mcp.client.McpClientBase;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.StdioTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestToolStdio extends TestToolBase {
    @BeforeAll
    static void setup() {
        Map<String, String> config = getConfig();
        McpTransport transport = StdioTransport.builder()
                .command(Arrays.asList(config.get("cmd_path"), "-token", config.get("token")))
                .logEvents(true)
                .build();

        mcpClient = new McpClientBase.Builder()
                .transport(transport)
                .toolExecutionTimeout(Duration.ofSeconds(4))
                .build();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mcpClient != null)
            mcpClient.close();
    }

    static Map<String, String> map;

    public static Map<String, String> getConfig() {
        if (map != null)
            return map;

        Properties properties = new Properties();

        try (InputStream input = TestToolStdio.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null)
                System.out.println("无法找到 config.properties 文件");
            else {
                properties.load(input);
                // 将 Properties 转换为 Map
                map = new HashMap<>();
                for (String key : properties.stringPropertyNames())
                    map.put(key, properties.getProperty(key));

                return map;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new HashMap<>();

    }
}
