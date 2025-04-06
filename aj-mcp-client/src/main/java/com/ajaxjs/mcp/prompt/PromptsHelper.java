package com.ajaxjs.mcp.prompt;


import com.ajaxjs.mcp.IllegalResponseException;
import com.ajaxjs.mcp.McpException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PromptsHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<Prompt> parsePromptRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");

            if (resultNode.has("prompts")) {
                List<Prompt> promptRefs = new ArrayList<>();
                for (JsonNode promptNode : resultNode.get("prompts"))
                    promptRefs.add(OBJECT_MAPPER.convertValue(promptNode, Prompt.class));

                return promptRefs;
            } else {
                log.warn("Result does not contain 'prompts' element: {}", resultNode);
                throw new IllegalResponseException("Result does not contain 'prompts' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }

    public static GetPromptResult parsePromptContents(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);
        return OBJECT_MAPPER.convertValue(mcpMessage.get("result"), GetPromptResult.class);
    }
}
