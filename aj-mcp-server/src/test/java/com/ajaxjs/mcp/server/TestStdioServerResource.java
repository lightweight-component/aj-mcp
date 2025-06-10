package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStdioServerResource extends TestStdioServerBase {
    @Test
    void testList() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"resources/list\"}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"resources\":[{\"uri\":\"file:///text\",\"name\":\"text\",\"mimeType\":\"text/plain\",\"description\":\"A nice piece of text\"},{\"uri\":\"file:///blob\",\"name\":\"blob\",\"mimeType\":\"text/blob\",\"description\":\"A nice blob\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testListPage() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"resources/list\",\"params\":{\"cursor\":\"eyJwYWdlIjoxfQ==\"}}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"resources\":[{\"uri\":\"file:///text\",\"name\":\"text\",\"mimeType\":\"text/plain\",\"description\":\"A nice piece of text\"},{\"uri\":\"file:///blob\",\"name\":\"blob\",\"mimeType\":\"image/jpg\",\"description\":\"A nice pic\"}]}}\r\n";

        JsonNode jsonNode = JsonUtils.json2Node(expectedOutput);
        JsonNode jsonNode1 = jsonNode.get("result").get("resources");
        List list = JsonUtils.jsonNode2bean(jsonNode1, List.class);
        assertEquals(2, list.size());
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testListTemplate() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"resources/list\"}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"resources\":[{\"uri\":\"file:///text\",\"name\":\"text\",\"mimeType\":\"text/plain\",\"description\":\"A nice piece of text\"},{\"uri\":\"file:///blob\",\"name\":\"blob\",\"mimeType\":\"text/blob\",\"description\":\"A nice blob\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testGetResource() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"resources/read\",\"params\":{\"uri\":\"file:///blob\"}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"contents\":[{\"uri\":\"file:///blob\",\"mimeType\":\"image/jpg\",\"blob\":\"/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAAWAB4DASIAAhEBAxEB/8QAGQAAAgMBAAAAAAAAAAAAAAAAAAYFBwkI/8QAJBAAAgMAAgICAgMBAAAAAAAAAgMBBAUGBxITABEIFAkhIjH/xAAXAQADAQAAAAAAAAAAAAAAAAADBgcF/8QAIhEAAgICAgIDAQEAAAAAAAAAAQIDBAURACESIgYTQRQx/9oADAMBAAIRAxEAPwDIB/5Jd3W6T62pyenc42wVMuZPJ8/L2s+9FafJCipHSNvsX5SLDrRNog8v9mAwsYXmNHc6p4pg9jd49tZvQvHOcO07OLxWnjcs5RyTYUSfu8vE4vlPxquNFqjZrWLAbbmACLVdVvNmwSkhXRXFidKDUNqVWF2azWAlwJuVpS5bH12uX76xQlIvXJCw0yyakvMRCXX+SDg/LPyu4P1R3n1VWu8rDhmS/D7K67wfC1yDi923NM2aNbE85fbQiyiwqAqfTdOm/OsVyf8ApPFEcbOy1b+AotaTFUshemr3MpoK9crCWrQgkGKH75QY/tcABtDYJB5k5eaytqlCZzBTmMpsWSA7BwqhIi8mwgcn2ck9AgdnkJ1VW6570drUulfybfzbnubQZsB1z2TwXX6/1+SUKBKJscevRrbVLSsoqCUxMJ/ZF8VyZNKuTWBejefcudkgNvMyKzaWiyo+rNEQsLuwmRvm39yKmig3WEzZfWsvsiD7EisyFYtZmR+BnQna3IPyi655ZX4ZzjhnDuudhnId3kethbeVUBFRDkqxqjbtZDbFzbYwUspAbCKvNomBCVTM6t9453r7U5ZnpLLrVbZ5m4uCa1ErbZpStqTqsEZhnlPn+wsiU8PEogC8x+PWG+a28R8xm+Nz5p8zi7GMS/UtOY3kpWUlMU1Np4vSVZEAmTyAZNsO98XxapvlziY7MdmVq39R8Cjuih1Qq7J0Q2wRs7HOV6+ew1WK/vbbV9wDRtGlf+WQBfUeFN3snzP78ikDEfsIL+4mGvjGLpYF9Ojhbuxx7SI3VlaGLr26r1qmF+xZCsFLakiKZlLINR/9IImZiT4fJLknZofBtOraLK6q6k+p34sCN7/wgbH5ygiGKZSksayIxIKuAyka32D1y/8AM572xUoIdb7E19Wpll7l17aaIEAgC1h4EmoIsYEBEgbYkl/3Al4zMSia9dXY3lvaFy4vXsWXTcvwlHvtSp9lBCwZNiRCfWshgFhMSMz9xBSMHw+J9WeWGdp4nMcolRQ6AL6sCGGgPHR0N9fnD1cJiYJVnhx9WKZkAaVIwshB8dguPbR/Rvn/2Q==\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }
}
