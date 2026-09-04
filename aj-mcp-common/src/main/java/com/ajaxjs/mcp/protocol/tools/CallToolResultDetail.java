package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.common.Content;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents call tool result detail.
 */
@Data
public class CallToolResultDetail {
    /**
     * Holds the is error value.
     */
    Boolean isError = false;

    /**
     * Holds the content value.
     */
    List<Content> content;

    /**
     * Structured result introduced in protocol revision 2025-06-18.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, Object> structuredContent;
}