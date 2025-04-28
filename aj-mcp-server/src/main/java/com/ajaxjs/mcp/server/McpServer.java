package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.protocol.McpRequestRaw;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.utils.ping.PingResponse;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

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

    public McpResponse processMessage(McpRequestRaw request) {
        switch (request.getMethod()) {
            case Methods.INITIALIZE:
                JsonNode jsonNode = request.getJsonNode();

                return initialize(request.getId(), jsonNode);
            case Methods.PING:
//                McpResponse resp = new McpResponse();
//                resp.setId(request.getId());
//                resp.setResult(new HashMap<>());
                PingResponse resp = new PingResponse();
                resp.setId(request.getId());

                return resp;
            default:
                throw new JsonRpcErrorException(request.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method " + request.getMethod() + " not found.");
        }
    }
}