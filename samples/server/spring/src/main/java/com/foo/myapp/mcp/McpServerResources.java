package com.foo.myapp.mcp;

import com.ajaxjs.mcp.protocol.resource.ResourceContentBinary;
import com.ajaxjs.mcp.protocol.resource.ResourceContentText;
import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Resource;
import com.ajaxjs.mcp.server.feature.annotation.ResourceTemplate;
import com.ajaxjs.mcp.server.feature.annotation.ResourceTemplateArg;

/**
 * Represents mcp server resources.
 */
@McpService
public class McpServerResources {
    /**
     * Executes the blob operation.
     * @return the result of the blob operation.
     */
    @Resource(uri = "demo://image", description = "A one-pixel PNG served without external files", mimeType = "image/png")
    public ResourceContentBinary blob() {
        ResourceContentBinary content = new ResourceContentBinary();
        content.setUri("demo://image");
        content.setMimeType("image/png");
        content.setBlob(SampleMcpContent.TRANSPARENT_PNG);

        return content;
    }

    /**
     * Executes the text operation.
     * @return the result of the text operation.
     */
    @Resource(uri = "demo://welcome", description = "A small text resource", mimeType = "text/plain")
    public ResourceContentText text() {
        ResourceContentText content = new ResourceContentText();
        content.setUri("demo://welcome");
        content.setMimeType("text/plain");
        content.setText("Welcome to the AJ-MCP Spring sample.");

        return content;
    }
    /**
     * Creates a text resource from the name embedded in its URI template.
     *
     * @param name the name decoded from the resource URI.
     * @return the generated greeting resource.
     */
    @ResourceTemplate(name = "greeting", title = "Personal greeting",
            description = "Creates a greeting for the name embedded in the URI", uriTemplate = "demo://greeting/{name}",
            mimeType = "text/plain")
    public ResourceContentText greeting(@ResourceTemplateArg(name = "name") String name) {
        ResourceContentText content = new ResourceContentText();
        content.setUri("demo://greeting/" + name);
        content.setMimeType("text/plain");
        content.setText("Hello, " + name + "!");
        return content;
    }
}
