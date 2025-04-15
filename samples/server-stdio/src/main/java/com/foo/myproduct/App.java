package com.foo.myproduct;

import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Scanner;

public class App {
    public static final String VERSION = "2.0";

    public static final int RESOURCE_NOT_FOUND = -32002;

    public static final int INTERNAL_ERROR = -32603;
    public static final int INVALID_PARAMS = -32602;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_REQUEST = -32600;
    public static final int PARSE_ERROR = -32700;

    public static final int SECURITY_ERROR = -32001;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String json = scanner.nextLine();
                JsonNode jsonNode = null;

                try {
                    jsonNode = JsonUtils.OBJECT_MAPPER.readTree(json);
                } catch (JsonProcessingException e) {
                    System.out.println("json err ");
//                    throw new RuntimeException(e);
                }
                String jsonrpc = jsonNode.asText();

                System.out.println("json: " + json);
                if (!VERSION.equals(jsonrpc)) {
//                    responder.sendError(id, INVALID_REQUEST, "Invalid jsonrpc version: " + jsonrpc);
//                    return false;
                }

            }
        }
    }
}
