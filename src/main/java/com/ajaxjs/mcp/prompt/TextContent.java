package com.ajaxjs.mcp.prompt;

import lombok.Data;

@Data
public class TextContent implements PromptContent {
    String text;

    @Override
    public Type type() {
        return Type.TEXT;
    }

//    @Override
//    public Content toContent() {
//        return TextContent.from(text);
//    }
}
