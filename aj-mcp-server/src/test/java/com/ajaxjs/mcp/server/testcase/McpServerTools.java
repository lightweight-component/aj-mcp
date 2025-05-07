package com.ajaxjs.mcp.server.testcase;

import com.ajaxjs.mcp.protocol.common.Content;
import com.ajaxjs.mcp.protocol.common.ContentImage;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.server.common.ServerUtils;
import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;
import com.ajaxjs.mcp.server.feature.annotation.ToolArg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@McpService
public class McpServerTools {
    @Tool(description = "Echoes a string")
    public String echoString(@ToolArg(description = "The string to be echoed") String input) {
        return input;
    }

    @Tool(description = "Echoes an integer")
    public String echoInteger(@ToolArg(description = "The integer to be echoed") Integer input) {
        return String.valueOf(input);
    }

    @Tool(description = "Echoes a boolean")
    public String echoBoolean(@ToolArg(description = "The boolean to be echoed") Boolean input) {
        return input.toString();
    }

    @Tool(description = "Takes 10 seconds to complete")
    public String longOperation() throws Exception {
        TimeUnit.SECONDS.sleep(5);
        return "ok";
    }

    @Tool(description = "Throws a business error")
    public String error() throws Exception {
        throw new RuntimeException("business error");
    }

//    @Tool(description = "Returns a response as an error")
//    public ToolResponse errorResponse() throws Exception {
//        List<TextContent> lst = new ArrayList<>();
//        lst.add(new TextContent("This is an actual error"));
//
//        return new ToolResponse(true, lst);
//    }

    @Tool(description = "A nice pic")
    public ContentImage image() {
        String base64EncodedImage = ServerUtils.encodeImageToBase64("bird-eye.jpg");

        ContentImage content = new ContentImage();
        content.setMimeType("image/jpg");
        content.setData(base64EncodedImage);

        return content;
    }

    @Tool(description = "List ALL")
    public List<Content> getAll() {
        String base64EncodedImage = ServerUtils.encodeImageToBase64("bird-eye.jpg");

        ContentImage imgContent = new ContentImage();
        imgContent.setMimeType("image/jpg");
        imgContent.setData(base64EncodedImage);

        ContentText textContent = new ContentText("Hello World");

        List<Content> list = new ArrayList<>();
        list.add(imgContent);
        list.add(textContent);

        return list;
    }
}
