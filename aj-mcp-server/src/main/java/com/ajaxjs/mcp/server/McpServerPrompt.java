package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRaw;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.prompt.GetPromptRequestList;
import com.ajaxjs.mcp.protocol.prompt.GetPromptResultList;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public abstract class McpServerPrompt extends McpServerResource {
    McpResponse promptList(McpRequestRaw requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        GetPromptRequestList request = new GetPromptRequestList();
        request.setId(requestRaw.getId());

        if (jsonNode.has("params"))
            request.setParams(JsonUtils.jsonNode2bean(jsonNode.get("params"), Cursor.class));

        // get info from RAM
        List<PromptItem> prompts = new ArrayList<>();
        for (String name : FeatureMgr.PROMPT_STORE.keySet()) {
            PromptItem promptItem = FeatureMgr.PROMPT_STORE.get(name).getPrompt();
            prompts.add(promptItem);
        }

        GetPromptResultList.PromptResult resultList = new GetPromptResultList.PromptResult(prompts);
        GetPromptResultList result = new GetPromptResultList(resultList);
        result.setId(requestRaw.getId());

        return result;
    }
}
