package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Listing Tools
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetToolListResult extends McpResponse {
    private ToolList result;

    public GetToolListResult(ToolList result) {
        this.result = result;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolList {
        List<ToolItem> tools;
    }
}
