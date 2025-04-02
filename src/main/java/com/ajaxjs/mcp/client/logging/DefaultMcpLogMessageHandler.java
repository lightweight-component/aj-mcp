package com.ajaxjs.mcp.client.logging;

import lombok.extern.slf4j.Slf4j;

/**
 * The default implementation of {@link McpLogMessageHandler} that simply forwards
 * MCP log notifications to the SLF4J logger.
 */
@Slf4j
public class DefaultMcpLogMessageHandler implements McpLogMessageHandler {
    @Override
    public void handleLogMessage(McpLogMessage message) {
        if (message.getLevel() == null) {
            log.warn("Received MCP log message with unknown level: {}", message.getData());
            return;
        }

        switch (message.getLevel()) {
            case DEBUG:
                log.debug("MCP logger: {}: {}", message.getLogger(), message.getData());
                break;
            case INFO:
            case NOTICE:
                log.info("MCP logger: {}: {}", message.getLogger(), message.getData());
                break;
            case WARNING:
                log.warn("MCP logger: {}: {}", message.getLogger(), message.getData());
                break;
            case ERROR:
            case CRITICAL:
            case ALERT:
            case EMERGENCY:
                log.error("MCP logger: {}: {}", message.getLogger(), message.getData());
                break;
            default:
                // 可选：处理未知的日志级别
                log.warn("MCP logger: Unknown log level: {}", message.getLevel());
                break;
        }


    }
}
