package com.foo.myproduct;

import com.ajaxjs.mcp.protocol.common.ContentEmbeddedResource;
import com.ajaxjs.mcp.protocol.common.ContentEmbeddedResourceDetail;
import com.ajaxjs.mcp.protocol.common.ContentImage;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.prompt.PromptMessage;
import com.ajaxjs.mcp.protocol.prompt.Role;
import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Prompt;
import com.ajaxjs.mcp.server.feature.annotation.PromptArg;

import java.util.Arrays;
import java.util.List;

/**
 * Represents mcp server prompts.
 */
@McpService
public class McpServerPrompts {
    /**
     * Executes the basic operation.
     * @return the result of the basic operation.
     */
    @Prompt(description = "Basic simple prompt")
    public PromptMessage basic() {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello"));

        return message;
    }

    /**
     * Executes the multi operation.
     * @return the result of the multi operation.
     */
    @Prompt(description = "Prompt that returns two messages")
    public List<PromptMessage> multi() {
        PromptMessage message1 = new PromptMessage();
        message1.setRole(Role.USER);
        message1.setContent(new ContentText("first"));

        PromptMessage message2 = new PromptMessage();
        message2.setRole(Role.USER);
        message2.setContent(new ContentText("second"));

        return Arrays.asList(message1, message2);
    }

    /**
     * Executes the parametrized operation.
     * @param name the name value.
     * @return the result of the parametrized operation.
     */
    @Prompt(description = "Parametrized prompt")
    public PromptMessage parametrized(@PromptArg(description = "The name") String name) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello " + name));

        return message;
    }

    /**
     * Executes the image operation.
     * @return the result of the image operation.
     */
    @Prompt(description = "Prompt that returns an image")
    public PromptMessage image() {
        ContentImage image = new ContentImage();
        image.setMimeType("image/png");
        image.setData(SampleMcpContent.TRANSPARENT_PNG);

        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(image);

        return message;
    }

    /**
     * Executes the embedded binary resource operation.
     * @return the result of the embedded binary resource operation.
     */
    @Prompt(description = "Prompt that returns an embedded binary resource")
    public PromptMessage embeddedBinaryResource() {
        ContentEmbeddedResource embeddedResource = new ContentEmbeddedResource();
        ContentEmbeddedResourceDetail resource = new ContentEmbeddedResourceDetail();
        resource.setUri("file:///embedded-blob");
        resource.setMimeType("application/octet-stream");
        resource.setBlob(SampleMcpContent.TRANSPARENT_PNG);

        embeddedResource.setResource(resource);

        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(embeddedResource);

        return message;
    }
}
