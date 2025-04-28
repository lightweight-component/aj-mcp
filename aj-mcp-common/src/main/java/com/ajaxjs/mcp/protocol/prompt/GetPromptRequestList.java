package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpRequestRaw;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.PROMPTS_LIST;

/**
 * Listing Prompts
 * To retrieve available prompts, clients send a prompts/list request. This operation supports pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetPromptRequestList extends McpRequestRaw {
    String method = PROMPTS_LIST;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    Cursor params;
}
