package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * MCP Server Tools
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServer extends McpServerPrompt {
    public void start() {
        log.info("MCP 服务器已启动，等待输入...");
        transport.start();
    }

    public McpResponse processMessage(McpRequest request) {
        switch (request.getMethod()) {
            case Methods.INITIALIZE:
                JsonNode jsonNode = request.getJsonNode();

                return initialize(request.getId(), jsonNode);
            case Methods.PING:
                McpResponse resp = new McpResponse();
                resp.setId(request.getId());
                resp.setResult(new HashMap<>());

                return resp;
            default:
                throw new JsonRpcErrorException(request.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method " + request.getMethod() + " not found.");
        }
    }
}