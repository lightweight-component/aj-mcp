package com.foo.streamable.features;

import com.ajaxjs.mcp.protocol.resource.ResourceContentText;
import com.ajaxjs.mcp.server.feature.annotation.*;

@McpService
public class DemoResources {
    @Resource(uri = "demo://welcome", description = "Local sample introduction", mimeType = "text/plain")
    public ResourceContentText welcome() {
        ResourceContentText content = new ResourceContentText();
        content.setUri("demo://welcome"); content.setMimeType("text/plain");
        content.setText("Welcome to the Streamable HTTP sample.");

        return content;
    }
}
