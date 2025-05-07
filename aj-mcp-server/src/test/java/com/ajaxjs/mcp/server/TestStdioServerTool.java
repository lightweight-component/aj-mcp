package com.ajaxjs.mcp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStdioServerTool extends TestStdioServerBase {
    @Test
    void testList() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/list\"}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"tools\":[{\"name\":\"longOperation\",\"description\":\"Takes 10 seconds to complete\",\"inputSchema\":null},{\"name\":\"image\",\"description\":\"A nice pic\",\"inputSchema\":null},{\"name\":\"getAll\",\"description\":\"List ALL\",\"inputSchema\":null},{\"name\":\"echoBoolean\",\"description\":\"Echoes a boolean\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"input\":{\"type\":\"Boolean\",\"description\":\"The boolean to be echoed\"}},\"required\":[\"input\"]}},{\"name\":\"echoInteger\",\"description\":\"Echoes an integer\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"input\":{\"type\":\"Number\",\"description\":\"The integer to be echoed\"}},\"required\":[\"input\"]}},{\"name\":\"error\",\"description\":\"Throws a business error\",\"inputSchema\":null},{\"name\":\"echoString\",\"description\":\"Echoes a string\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"input\":{\"type\":\"string\",\"description\":\"The string to be echoed\"}},\"required\":[\"input\"]}}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testEchoString() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"echoString\",\"arguments\":{\"input\":\"hi\"}}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"isError\":false,\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testEchoInteger() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"echoInteger\",\"arguments\":{\"input\":888}}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"isError\":false,\"content\":[{\"type\":\"text\",\"text\":\"888\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

//    @Test
//    void testEchoInteger2() {
//        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"echoInteger\",\"arguments\":{\"input\":888.99}}}\n");
//
//        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"isError\":false,\"content\":[{\"type\":\"text\",\"text\":\"888.99\"}]}}\r\n";
//        assertEquals(expectedOutput, testOut.toString());
//    }

    @Test
    void testEchoBoolean() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"echoBoolean\",\"arguments\":{\"input\":true}}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"isError\":false,\"content\":[{\"type\":\"text\",\"text\":\"true\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testLongOperation() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"longOperation\"}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"isError\":false,\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testImage() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"image\"}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"isError\":false,\"content\":[{\"type\":\"image\",\"data\":\"/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAAWAB4DASIAAhEBAxEB/8QAGQAAAgMBAAAAAAAAAAAAAAAAAAYFBwkI/8QAJBAAAgMAAgICAgMBAAAAAAAAAgMBBAUGBxITABEIFAkhIjH/xAAXAQADAQAAAAAAAAAAAAAAAAADBgcF/8QAIhEAAgICAgIDAQEAAAAAAAAAAQIDBAURACESIgYTQRQx/9oADAMBAAIRAxEAPwDIB/5Jd3W6T62pyenc42wVMuZPJ8/L2s+9FafJCipHSNvsX5SLDrRNog8v9mAwsYXmNHc6p4pg9jd49tZvQvHOcO07OLxWnjcs5RyTYUSfu8vE4vlPxquNFqjZrWLAbbmACLVdVvNmwSkhXRXFidKDUNqVWF2azWAlwJuVpS5bH12uX76xQlIvXJCw0yyakvMRCXX+SDg/LPyu4P1R3n1VWu8rDhmS/D7K67wfC1yDi923NM2aNbE85fbQiyiwqAqfTdOm/OsVyf8ApPFEcbOy1b+AotaTFUshemr3MpoK9crCWrQgkGKH75QY/tcABtDYJB5k5eaytqlCZzBTmMpsWSA7BwqhIi8mwgcn2ck9AgdnkJ1VW6570drUulfybfzbnubQZsB1z2TwXX6/1+SUKBKJscevRrbVLSsoqCUxMJ/ZF8VyZNKuTWBejefcudkgNvMyKzaWiyo+rNEQsLuwmRvm39yKmig3WEzZfWsvsiD7EisyFYtZmR+BnQna3IPyi655ZX4ZzjhnDuudhnId3kethbeVUBFRDkqxqjbtZDbFzbYwUspAbCKvNomBCVTM6t9453r7U5ZnpLLrVbZ5m4uCa1ErbZpStqTqsEZhnlPn+wsiU8PEogC8x+PWG+a28R8xm+Nz5p8zi7GMS/UtOY3kpWUlMU1Np4vSVZEAmTyAZNsO98XxapvlziY7MdmVq39R8Cjuih1Qq7J0Q2wRs7HOV6+ew1WK/vbbV9wDRtGlf+WQBfUeFN3snzP78ikDEfsIL+4mGvjGLpYF9Ojhbuxx7SI3VlaGLr26r1qmF+xZCsFLakiKZlLINR/9IImZiT4fJLknZofBtOraLK6q6k+p34sCN7/wgbH5ygiGKZSksayIxIKuAyka32D1y/8AM572xUoIdb7E19Wpll7l17aaIEAgC1h4EmoIsYEBEgbYkl/3Al4zMSia9dXY3lvaFy4vXsWXTcvwlHvtSp9lBCwZNiRCfWshgFhMSMz9xBSMHw+J9WeWGdp4nMcolRQ6AL6sCGGgPHR0N9fnD1cJiYJVnhx9WKZkAaVIwshB8dguPbR/Rvn/2Q==\",\"mimeType\":\"image/jpg\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testGetAll() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"getAll\"}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"isError\":false,\"content\":[{\"type\":\"image\",\"data\":\"/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAAWAB4DASIAAhEBAxEB/8QAGQAAAgMBAAAAAAAAAAAAAAAAAAYFBwkI/8QAJBAAAgMAAgICAgMBAAAAAAAAAgMBBAUGBxITABEIFAkhIjH/xAAXAQADAQAAAAAAAAAAAAAAAAADBgcF/8QAIhEAAgICAgIDAQEAAAAAAAAAAQIDBAURACESIgYTQRQx/9oADAMBAAIRAxEAPwDIB/5Jd3W6T62pyenc42wVMuZPJ8/L2s+9FafJCipHSNvsX5SLDrRNog8v9mAwsYXmNHc6p4pg9jd49tZvQvHOcO07OLxWnjcs5RyTYUSfu8vE4vlPxquNFqjZrWLAbbmACLVdVvNmwSkhXRXFidKDUNqVWF2azWAlwJuVpS5bH12uX76xQlIvXJCw0yyakvMRCXX+SDg/LPyu4P1R3n1VWu8rDhmS/D7K67wfC1yDi923NM2aNbE85fbQiyiwqAqfTdOm/OsVyf8ApPFEcbOy1b+AotaTFUshemr3MpoK9crCWrQgkGKH75QY/tcABtDYJB5k5eaytqlCZzBTmMpsWSA7BwqhIi8mwgcn2ck9AgdnkJ1VW6570drUulfybfzbnubQZsB1z2TwXX6/1+SUKBKJscevRrbVLSsoqCUxMJ/ZF8VyZNKuTWBejefcudkgNvMyKzaWiyo+rNEQsLuwmRvm39yKmig3WEzZfWsvsiD7EisyFYtZmR+BnQna3IPyi655ZX4ZzjhnDuudhnId3kethbeVUBFRDkqxqjbtZDbFzbYwUspAbCKvNomBCVTM6t9453r7U5ZnpLLrVbZ5m4uCa1ErbZpStqTqsEZhnlPn+wsiU8PEogC8x+PWG+a28R8xm+Nz5p8zi7GMS/UtOY3kpWUlMU1Np4vSVZEAmTyAZNsO98XxapvlziY7MdmVq39R8Cjuih1Qq7J0Q2wRs7HOV6+ew1WK/vbbV9wDRtGlf+WQBfUeFN3snzP78ikDEfsIL+4mGvjGLpYF9Ojhbuxx7SI3VlaGLr26r1qmF+xZCsFLakiKZlLINR/9IImZiT4fJLknZofBtOraLK6q6k+p34sCN7/wgbH5ygiGKZSksayIxIKuAyka32D1y/8AM572xUoIdb7E19Wpll7l17aaIEAgC1h4EmoIsYEBEgbYkl/3Al4zMSia9dXY3lvaFy4vXsWXTcvwlHvtSp9lBCwZNiRCfWshgFhMSMz9xBSMHw+J9WeWGdp4nMcolRQ6AL6sCGGgPHR0N9fnD1cJiYJVnhx9WKZkAaVIwshB8dguPbR/Rvn/2Q==\",\"mimeType\":\"image/jpg\"},{\"type\":\"text\",\"text\":\"Hello World\"}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }
}
