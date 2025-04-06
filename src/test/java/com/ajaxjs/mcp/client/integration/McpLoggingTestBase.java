package com.ajaxjs.mcp.client.integration;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.client.logging.McpLogLevel;
import com.ajaxjs.mcp.client.logging.McpLogMessage;
import com.ajaxjs.mcp.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class McpLoggingTestBase {
    static McpClient mcpClient;
    static TestMcpLogHandler logMessageHandler;

    @BeforeEach
    public void clearMessages() {
        logMessageHandler.clearMessages();
    }

    @Test
    public void receiveInfoLogMessage() throws TimeoutException {
        String result = mcpClient.executeTool(ToolExecutionRequest.builder().arguments("{}").name("info").build());
        assertEquals("ok", result, "The result should be 'ok'");

        List<McpLogMessage> receivedMessages = logMessageHandler.waitForAtLeastOneMessageAndGet(Duration.ofSeconds(10));
        assertEquals(1, receivedMessages.size(), "Expected exactly one log message");

        McpLogMessage message = receivedMessages.get(0);
        assertEquals(McpLogLevel.INFO, message.getLevel(), "Log level should be INFO");
        assertEquals("tool:info", message.getLogger(), "Logger name should be 'tool:info'");
        assertEquals("HELLO", message.getData().asText(), "Log message data should be 'HELLO'");
    }

    @Test
    public void receiveDebugLogMessage() throws TimeoutException {
        String result = mcpClient.executeTool(ToolExecutionRequest.builder().arguments("{}").name("debug").build());
        assertEquals("ok", result, "The result should be 'ok'");

        List<McpLogMessage> receivedMessages = logMessageHandler.waitForAtLeastOneMessageAndGet(Duration.ofSeconds(10));
        assertEquals(1, receivedMessages.size(), "Expected exactly one log message");

        McpLogMessage message = receivedMessages.get(0);
        assertEquals(McpLogLevel.DEBUG, message.getLevel(), "Log level should be DEBUG");
        assertEquals("tool:debug", message.getLogger(), "Logger name should be 'tool:debug'");
        assertEquals("HELLO DEBUG", message.getData().asText(), "Log message data should be 'HELLO DEBUG'");
    }
}
