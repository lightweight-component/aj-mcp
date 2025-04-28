package com.ajaxjs.mcp.client.protocol.prompt;

import com.ajaxjs.mcp.message.Content;
import lombok.Data;

@Data
public class PromptImageContent implements PromptContent {
    String data;

    String mimeType;

    @Override
    public Type type() {
        return Type.IMAGE;
    }

    @Override
    public Content toContent() {
        return null;
//        return ImageContent.from(data, mimeType);
    }
}
