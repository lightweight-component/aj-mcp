package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.ajaxjs.mcp.protocol.tools.JsonSchemaProperty;
import com.ajaxjs.mcp.protocol.tools.ToolSpecification;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestToolSpecification {
    String toolText = "[ {\n" +
            "                      \"name\" : \"operation\",\n" +
            "                      \"description\" : \"Super operation\",\n" +
            "                      \"inputSchema\" : {\n" +
            "                        \"type\" : \"object\",\n" +
            "                        \"properties\" : {\n" +
            "                          \"stringParameter\" : {\n" +
            "                            \"type\" : \"string\",\n" +
            "                            \"description\" : \"Message to echo\"\n" +
            "                          },\n" +
            "                          \"enumParameter\" : {\n" +
            "                            \"type\" : \"string\",\n" +
            "                            \"description\" : \"The protocol to use\",\n" +
            "                            \"enum\": [\n" +
            "                                \"http\",\n" +
            "                                \"https\"\n" +
            "                            ]\n" +
            "                          },\n" +
            "                          \"numberParameter\": {\n" +
            "                            \"type\": \"number\",\n" +
            "                            \"description\": \"A number\"\n" +
            "                          },\n" +
            "                          \"integerParameter\": {\n" +
            "                            \"type\": \"integer\",\n" +
            "                            \"description\": \"An integer\"\n" +
            "                          },\n" +
            "                          \"booleanParameter\": {\n" +
            "                            \"type\": \"boolean\",\n" +
            "                            \"description\": \"A boolean\"\n" +
            "                          },\n" +
            "                          \"arrayParameter\": {\n" +
            "                              \"type\": \"array\",\n" +
            "                              \"description\": \"An array of strings\",\n" +
            "                              \"items\": {\n" +
            "                                \"type\": \"string\"\n" +
            "                              }\n" +
            "                          }\n" +
            "                        },\n" +
            "                        \"required\" : [ \"stringParameter\" ],\n" +
            "                        \"additionalProperties\" : false\n" +
            "                      }\n" +
            "                    } ]";

    @Test
    void toolWithSimpleParams() {
        // Parse JSON into ArrayNode and extract ToolSpecifications
        ArrayNode json = JsonUtils.fromJson(toolText, ArrayNode.class);
        List<ToolSpecification> toolSpecifications = McpClient.toolSpecificationListFromMcpResponse(json);

        // Validate the size of the tool specifications list
        assertEquals(1, toolSpecifications.size(), "Expected exactly one tool specification");

        // Validate the first tool specification
        ToolSpecification toolSpecification = toolSpecifications.get(0);
        assertNotNull(toolSpecification, "Tool specification should not be null");
        assertEquals("operation", toolSpecification.getName(), "Name mismatch for tool specification");
        assertEquals("Super operation", toolSpecification.getDescription(), "Description mismatch for tool specification");

        // Validate parameters
        JsonSchema parameters = toolSpecification.getInputSchema();

        assertNotNull(parameters, "Parameters should not be null");
        assertEquals(6, parameters.getProperties().size(), "Expected exactly 6 properties in parameters");
        assertEquals(1, parameters.getRequired().size(), "Expected exactly 1 required parameter");
        assertEquals("stringParameter", parameters.getRequired().get(0), "First required parameter should be 'stringParameter'");

        // Validate stringParameter
        Map<String, JsonSchemaProperty> messageParameter = parameters.getProperties();
        assertNotNull(messageParameter, "stringParameter should not be null");
        assertEquals("Message to echo", messageParameter.get("stringParameter").getDescription(), "Description mismatch for stringParameter");
    }

    String objText = "[\n" +
            "                  {\n" +
            "                    \"name\": \"operation\",\n" +
            "                    \"description\": \"Super operation\",\n" +
            "                    \"inputSchema\": {\n" +
            "                      \"type\": \"object\",\n" +
            "                      \"properties\": {\n" +
            "                        \"complexParameter\": {\n" +
            "                          \"type\": \"object\",\n" +
            "                          \"description\": \"A complex parameter\",\n" +
            "                          \"properties\": {\n" +
            "                            \"nestedString\": {\n" +
            "                              \"type\": \"string\",\n" +
            "                              \"description\": \"A nested string\"\n" +
            "                            },\n" +
            "                            \"nestedNumber\": {\n" +
            "                              \"type\": \"number\",\n" +
            "                              \"description\": \"A nested number\"\n" +
            "                            }\n" +
            "                          }\n" +
            "                        }\n" +
            "                      },\n" +
            "                      \"additionalProperties\": false\n" +
            "                    }\n" +
            "                  }\n" +
            "                ]";

    @Test
    void toolWithObjectParam() {
        ArrayNode json = JsonUtils.fromJson(objText, ArrayNode.class);
        // Extract ToolSpecifications from the JSON response
        List<ToolSpecification> toolSpecifications = McpClient.toolSpecificationListFromMcpResponse(json);

        // Validate the size of the tool specifications list
        assertEquals(1, toolSpecifications.size(), "Expected exactly one tool specification");

        // Validate the first tool specification
        ToolSpecification toolSpecification = toolSpecifications.get(0);
        assertNotNull(toolSpecification, "Tool specification should not be null");
        assertEquals("operation", toolSpecification.getName(), "Name mismatch for tool specification");
        assertEquals("Super operation", toolSpecification.getDescription(), "Description mismatch for tool specification");

        // Validate parameters
        JsonSchema parameters = toolSpecification.getInputSchema();
        assertNotNull(parameters, "Parameters should not be null");
        assertEquals(1, parameters.getProperties().size(), "Expected exactly 1 property in parameters");

        // Validate complexParameter
        JsonSchemaProperty complexParameter = parameters.getProperties().get("complexParameter");
        assertNotNull(complexParameter, "complexParameter should not be null");
        assertEquals("A complex parameter", complexParameter.getDescription(), "Description mismatch for complexParameter");
        assertEquals(2, complexParameter.getProperties().size(), "Expected exactly 2 properties in complexParameter");

        // Validate nestedString
        JsonSchemaProperty nestedStringParameter = complexParameter.getProperties().get("nestedString");
        assertNotNull(nestedStringParameter, "nestedString should not be null");
        assertEquals("A nested string", nestedStringParameter.getDescription(), "Description mismatch for nestedString");

        // Validate nestedNumber
        JsonSchemaProperty nestedNumberParameter = complexParameter.getProperties().get("nestedNumber");
        assertNotNull(nestedNumberParameter, "nestedNumber should not be null");
        assertEquals("A nested number", nestedNumberParameter.getDescription(), "Description mismatch for nestedNumber");
    }

    String noParamsText = "[{\n" +
            "                    \"name\" : \"getTinyImage\",\n" +
            "                    \"description\" : \"Returns the MCP_TINY_IMAGE\",\n" +
            "                    \"inputSchema\" : {\n" +
            "                        \"type\" : \"object\",\n" +
            "                        \"properties\" : { },\n" +
            "                        \"additionalProperties\" : false,\n" +
            "                        \"$schema\" : \"http://json-schema.org/draft-07/schema#\"\n" +
            "                    }\n" +
            "                }]";

    @Test
    void toolWithNoParams() {

        ArrayNode json = JsonUtils.fromJson(noParamsText, ArrayNode.class);
        // Extract ToolSpecifications from the JSON response
        List<ToolSpecification> toolSpecifications = McpClient.toolSpecificationListFromMcpResponse(json);

        // Validate the size of the tool specifications list
        assertEquals(1, toolSpecifications.size(), "Expected exactly one tool specification");

        // Validate the first tool specification
        ToolSpecification toolSpecification = toolSpecifications.get(0);
        assertNotNull(toolSpecification, "Tool specification should not be null");
        assertEquals("getTinyImage", toolSpecification.getName(), "Name mismatch for tool specification");
        assertEquals("Returns the MCP_TINY_IMAGE", toolSpecification.getDescription(), "Description mismatch for tool specification");

        // Validate parameters
        JsonSchema inputSchema = toolSpecification.getInputSchema();
        assertNotNull(inputSchema, "Parameters should not be null");
        assertTrue(inputSchema.getProperties().isEmpty(), "Expected parameters to be empty");
    }
}
