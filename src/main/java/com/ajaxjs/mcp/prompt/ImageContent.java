package com.ajaxjs.mcp.prompt;

import lombok.Data;

@Data
public class ImageContent implements PromptContent {
    String data;

    String mimeType;

    @Override
    public Type type() {
        return Type.IMAGE;
    }

//    @Override
//    public Content toContent() {
//        return ImageContent.from(data, mimeType);
//    }
}
