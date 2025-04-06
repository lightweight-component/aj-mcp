package com.ajaxjs.mcp.prompt;

import com.ajaxjs.mcp.message.Content;
import com.ajaxjs.mcp.message.TextContent;
import com.ajaxjs.mcp.resource.ResourceContents;
import com.ajaxjs.mcp.resource.TextResourceContents;
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
            return TextContent.from(((TextResourceContents) resource).getText());
        else
            throw new UnsupportedOperationException("Representing blob embedded resources as Content is currently not supported");
    }
}
