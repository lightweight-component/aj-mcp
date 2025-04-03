package com.ajaxjs.mcp.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ToolExecutionRequest {
    private String id;
    private String name;
    private String arguments;
}
