package com.ajaxjs.mcp.prompt;

import com.ajaxjs.mcp.message.Content;
import com.ajaxjs.mcp.message.TextContent;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PromptTextContent implements PromptContent {
    String text;

    @Override
    public Type type() {
        return Type.TEXT;
    }

    @Override
    public Content toContent() {
        return TextContent.from(text);
    }

}
