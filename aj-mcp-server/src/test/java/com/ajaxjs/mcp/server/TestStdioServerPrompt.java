package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.server.feature.FeatureMgr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStdioServerPrompt extends TestStdioServerBase {
    @Test
    void testList() {
        FeatureMgr mgr = new FeatureMgr();
        mgr.init("com.ajaxjs.mcp.server.testcase");

        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"prompts/list\"}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"prompts\":[{\"name\":\"image\",\"description\":\"Prompt that returns an image\",\"arguments\":null},{\"name\":\"parametrized\",\"description\":\"Parametrized prompt\",\"arguments\":[{\"name\":\"name\",\"description\":\"The name\",\"required\":true}]},{\"name\":\"basic\",\"description\":\"Basic simple prompt\",\"arguments\":null},{\"name\":\"embeddedBinaryResource\",\"description\":\"Prompt that returns an embedded binary resource\",\"arguments\":null},{\"name\":\"multi\",\"description\":\"Prompt that returns two messages\",\"arguments\":null}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }
}
