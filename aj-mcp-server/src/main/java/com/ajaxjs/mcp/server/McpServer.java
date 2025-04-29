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

    public void scanPackage() {

    }

    public McpResponse processMessage(McpRequestRaw requestRaw) {
        switch (requestRaw.getMethod()) {
            case Methods.INITIALIZE:
                JsonNode jsonNode = requestRaw.getJsonNode();
                return initialize(requestRaw.getId(), jsonNode);
            case Methods.PROMPTS_LIST:
                return promptList(requestRaw);
            case Methods.PING:

//                McpResponse resp = new McpResponse();
//                resp.setId(request.getId());
//                resp.setResult(new HashMap<>());
                PingResponse resp = new PingResponse();
                resp.setId(requestRaw.getId());

                return resp;
            default:
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method " + requestRaw.getMethod() + " not found.");
        }
    }
}