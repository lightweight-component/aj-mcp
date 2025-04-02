package com.ajaxjs.mcp.tool;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToolExecutionRequest {
    private String id;
    private String name;
    private String arguments;
}
