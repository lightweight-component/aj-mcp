package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.tools.GetToolListResult;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.ajaxjs.mcp.protocol.utils.ping.PingResponse;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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

    public McpResponse processMessage(McpRequestRawInfo requestRaw) {
        switch (requestRaw.getMethod()) {
            case Methods.INITIALIZE:
                JsonNode jsonNode = requestRaw.getJsonNode();
                return initialize(requestRaw.getId(), jsonNode);
            case Methods.PING:
//                McpResponse resp = new McpResponse();
//                resp.setId(request.getId());
//                resp.setResult(new HashMap<>());
                PingResponse resp = new PingResponse();
                resp.setId(requestRaw.getId());

                return resp;
            case Methods.PROMPTS_LIST:
                return promptList(requestRaw);
            case Methods.PROMPTS_GET:
                return promptGet(requestRaw);
            case Methods.RESOURCES_LIST:
                return resourceList(requestRaw);
            case Methods.RESOURCES_READ:
                return resourceRead(requestRaw);
            case Methods.TOOLS_LIST:
                return toolList(requestRaw);
            case Methods.TOOLS_CALL:
//                return toolCall(requestRaw);
            default:
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method " + requestRaw.getMethod() + " not found.");
        }
    }

    McpResponse toolList(McpRequestRawInfo requestRaw) {
        List<ToolItem> tools = null;

        GetToolListResult result = new GetToolListResult();
        result.setId(requestRaw.getId());
        result.setResult(new GetToolListResult.ToolList(tools));

        return result;
    }

//    McpResponse toolCall(McpRequestRawInfo requestRaw) {}
}