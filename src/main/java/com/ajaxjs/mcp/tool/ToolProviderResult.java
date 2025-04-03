package com.ajaxjs.mcp.tool;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ToolProviderResult {
    private Map<ToolSpecification, ToolExecutor> tools;
}
