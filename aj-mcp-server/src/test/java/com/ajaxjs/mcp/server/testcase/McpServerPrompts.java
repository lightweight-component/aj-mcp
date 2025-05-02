package com.ajaxjs.mcp.server.testcase;

import com.ajaxjs.mcp.protocol.common.ContentEmbeddedResource;
import com.ajaxjs.mcp.protocol.common.ContentImage;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.prompt.*;
import com.ajaxjs.mcp.server.common.ServerUtils;
import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Prompt;
import com.ajaxjs.mcp.server.feature.annotation.PromptArg;

import java.util.Arrays;
import java.util.List;

@McpService
public class McpServerPrompts {
    @Prompt(description = "Basic simple prompt")
    public PromptMessage basic() {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello, how are you?"));

        return message;
    }

    @Prompt(description = "Prompt that returns two messages")
    List<PromptMessage> multi() {
        PromptMessage message1 = new PromptMessage();
        message1.setRole(Role.USER);
        message1.setContent(new ContentText("first"));

        PromptMessage message2 = new PromptMessage();
        message2.setRole(Role.USER);
        message2.setContent(new ContentText("second"));

        return Arrays.asList(message1, message2);
    }

    @Prompt(description = "Parametrized prompt")
    PromptMessage parametrized(@PromptArg(description = "The name") String name) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello " + name));

        return message;
    }

    @Prompt(description = "Prompt that returns an image")
    PromptMessage image() {
        String base64EncodedImage = ServerUtils.encodeImageToBase64("/com/ajaxjs/mcp/server/testcase/bird.jpg");
        ContentImage image = new ContentImage();
        image.setMimeType("image/jpg");
        image.setData(base64EncodedImage);

        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(image);

        return message;
    }

    @Prompt(description = "Prompt that returns an embedded binary resource")
    PromptMessage embeddedBinaryResource() {
        ContentEmbeddedResource embeddedResource = new ContentEmbeddedResource();
        ContentEmbeddedResource.Resource resource = new ContentEmbeddedResource.Resource();
        resource.setUri("file:///embedded-blob");
        resource.setMimeType("application/octet-stream");

        embeddedResource.setResource(resource);

        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(embeddedResource);

        return message;
    }
}
