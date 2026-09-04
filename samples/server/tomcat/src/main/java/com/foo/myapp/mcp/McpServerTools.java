package com.foo.myapp.mcp;

import com.ajaxjs.mcp.protocol.common.Content;
import com.ajaxjs.mcp.protocol.common.ContentImage;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;
import com.ajaxjs.mcp.server.feature.annotation.ToolArg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents mcp server tools.
 */
@McpService
public class McpServerTools {
    /**
     * Executes the echo string operation.
     * @param input the input value.
     * @return the result of the echo string operation.
     */
    @Tool(description = "Echoes a string")
    public String echoString(@ToolArg(description = "The string to be echoed") String input) {
        return input;
    }

    /**
     * Executes the echo integer operation.
     * @param input the input value.
     * @return the result of the echo integer operation.
     */
    @Tool(description = "Echoes an integer")
    public String echoInteger(@ToolArg(description = "The integer to be echoed") Integer input) {
        return String.valueOf(input);
    }

    /**
     * Executes the echo boolean operation.
     * @param input the input value.
     * @return the result of the echo boolean operation.
     */
    @Tool(description = "Echoes a boolean")
    public String echoBoolean(@ToolArg(description = "The boolean to be echoed") Boolean input) {
        return input.toString();
    }

    /**
     * Executes the long operation operation.
     * @return the result of the long operation operation.
     * @throws Exception if the operation cannot complete.
     */
    @Tool(description = "Takes 10 seconds to complete")
    public String longOperation() throws Exception {
        TimeUnit.SECONDS.sleep(5);
        return "ok";
    }

    /**
     * Executes the error operation.
     * @return the result of the error operation.
     * @throws Exception if the operation cannot complete.
     */
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

    /**
     * Executes the image operation.
     * @return the result of the image operation.
     */
    @Tool(description = "A nice pic")
    public ContentImage image() {
        ContentImage content = new ContentImage();
        content.setMimeType("image/png");
        content.setData(SampleMcpContent.TRANSPARENT_PNG);

        return content;
    }

    /**
     * Executes the get all operation.
     * @return the result of the get all operation.
     */
    @Tool(description = "List ALL")
    public List<Content> getAll() {
        ContentImage imgContent = new ContentImage();
        imgContent.setMimeType("image/png");
        imgContent.setData(SampleMcpContent.TRANSPARENT_PNG);

        ContentText textContent = new ContentText("Hello World");

        List<Content> list = new ArrayList<>();
        list.add(imgContent);
        list.add(textContent);

        return list;
    }
}
