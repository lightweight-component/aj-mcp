package com.ajaxjs.mcp.client.tool;

import com.ajaxjs.mcp.message.UserMessage;
import lombok.Data;

@Data
public class ToolProviderRequest {
    private Object chatMemoryId;

    private UserMessage userMessage;
}
