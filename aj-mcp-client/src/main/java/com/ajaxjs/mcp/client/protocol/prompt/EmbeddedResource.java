package com.ajaxjs.mcp.client.protocol.prompt;

import com.ajaxjs.mcp.client.protocol.resource.ResourceContents;
import com.ajaxjs.mcp.client.protocol.resource.TextResourceContents;
import com.ajaxjs.mcp.message.Content;
import com.ajaxjs.mcp.message.TextContent;
import lombok.Data;

@Data
public class EmbeddedResource implements PromptContent {
    ResourceContents resource;

    @Override
    public Type type() {
        return Type.RESOURCE;
    }

    @Override
    public Content toContent() {
        if (resource.getType().equals(ResourceContents.Type.TEXT))
            return new TextContent(((TextResourceContents) resource).getText());
        else
            throw new UnsupportedOperationException("Representing blob embedded resources as Content is currently not supported");
    }
}
