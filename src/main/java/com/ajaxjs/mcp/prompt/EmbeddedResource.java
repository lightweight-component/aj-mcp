package com.ajaxjs.mcp.prompt;

import com.ajaxjs.mcp.resource.ResourceContents;
import lombok.Data;

@Data
public class EmbeddedResource implements PromptContent {
    ResourceContents resource;

    @Override
    public Type type() {
        return Type.RESOURCE;
    }

//    @Override
//    public Content toContent() {
//        if (resource.getType().equals(ResourceContents.Type.TEXT))
//            return TextContent.from(((McpTextResourceContents) resource).text());
//        else
//            throw new UnsupportedOperationException("Representing blob embedded resources as Content is currently not supported");
//    }
}
