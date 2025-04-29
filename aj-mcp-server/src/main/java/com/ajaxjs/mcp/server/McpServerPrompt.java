package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRaw;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.prompt.GetPromptRequestList;
import com.ajaxjs.mcp.protocol.prompt.GetPromptResultList;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class McpServerPrompt extends McpServerResource {
    McpResponse promptList(McpRequestRaw requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        GetPromptRequestList request = new GetPromptRequestList();
        request.setId(requestRaw.getId());

        if (jsonNode.has("params"))
            request.setParams(JsonUtils.jsonNode2bean(jsonNode.get("params"), Cursor.class));

        GetPromptResultList result = new GetPromptResultList();
        result.setId(requestRaw.getId());

        return result;
    }
}
