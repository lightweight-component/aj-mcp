package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.tool.ToolSpecification;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.json.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class ToolSpecificationHelperTest {
    @Test
    void toolWithSimpleParams() throws JsonProcessingException {
        String text =

                "[ {\n" +
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
                        "                        \"additionalProperties\" : false,\n" +
                        "                        \"$schema\" : \"http://json-schema.org/draft-07/schema#\"\n" +
                        "                      }\n" +
                        "                    } ]";
        // Parse JSON into ArrayNode and extract ToolSpecifications
        ArrayNode json = JsonUtils.fromJson(text, ArrayNode.class);
        List<ToolSpecification> toolSpecifications = McpClient.toolSpecificationListFromMcpResponse(json);

        // Validate the size of the tool specifications list
        assertEquals(1, toolSpecifications.size(), "Expected exactly one tool specification");

        // Validate the first tool specification
        ToolSpecification toolSpecification = toolSpecifications.get(0);
        assertNotNull(toolSpecification, "Tool specification should not be null");
        assertEquals("operation", toolSpecification.getName(), "Name mismatch for tool specification");
        assertEquals("Super operation", toolSpecification.getDescription(), "Description mismatch for tool specification");

        // Validate parameters
        JsonObjectSchema parameters = toolSpecification.getParameters();
        assertNotNull(parameters, "Parameters should not be null");
        assertEquals(6, parameters.getProperties().size(), "Expected exactly 6 properties in parameters");
        assertEquals(1, parameters.getRequired().size(), "Expected exactly 1 required parameter");
        assertEquals("stringParameter", parameters.getRequired().get(0), "First required parameter should be 'stringParameter'");

        // Validate stringParameter
        JsonStringSchema messageParameter = (JsonStringSchema) parameters.getProperties().get("stringParameter");
        assertNotNull(messageParameter, "stringParameter should not be null");
        assertEquals("Message to echo", messageParameter.getDescription(), "Description mismatch for stringParameter");

        // Validate enumParameter
        JsonEnumSchema enumParameter = (JsonEnumSchema) parameters.getProperties().get("enumParameter");
        assertNotNull(enumParameter, "enumParameter should not be null");
        assertEquals("The protocol to use", enumParameter.getDescription(), "Description mismatch for enumParameter");
        assertArrayEquals(new String[]{"http", "https"}, enumParameter.getEnumValues().toArray(), "Enum values mismatch for enumParameter");

        // Validate numberParameter
        JsonNumberSchema numberParameter = (JsonNumberSchema) parameters.getProperties().get("numberParameter");
        assertNotNull(numberParameter, "numberParameter should not be null");
        assertEquals("A number", numberParameter.getDescription(), "Description mismatch for numberParameter");

        // Validate integerParameter
        JsonIntegerSchema integerParameter = (JsonIntegerSchema) parameters.getProperties().get("integerParameter");
        assertNotNull(integerParameter, "integerParameter should not be null");
        assertEquals("An integer", integerParameter.getDescription(), "Description mismatch for integerParameter");

        // Validate booleanParameter
        JsonBooleanSchema booleanParameter = (JsonBooleanSchema) parameters.getProperties().get("booleanParameter");
        assertNotNull(booleanParameter, "booleanParameter should not be null");
        assertEquals("A boolean", booleanParameter.getDescription(), "Description mismatch for booleanParameter");

        // Validate arrayParameter
        JsonArraySchema arrayParameter = (JsonArraySchema) parameters.getProperties().get("arrayParameter");
        assertNotNull(arrayParameter, "arrayParameter should not be null");
        assertEquals("An array of strings", arrayParameter.getDescription(), "Description mismatch for arrayParameter");
    }

    @Test
    void toolWithObjectParam() throws JsonProcessingException {
        String text = "[\n" +
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
        ArrayNode json = JsonUtils.fromJson(text, ArrayNode.class);
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
        JsonObjectSchema parameters = toolSpecification.getParameters();
        assertNotNull(parameters, "Parameters should not be null");
        assertEquals(1, parameters.getProperties().size(), "Expected exactly 1 property in parameters");

        // Validate complexParameter
        JsonObjectSchema complexParameter = (JsonObjectSchema) parameters.getProperties().get("complexParameter");
        assertNotNull(complexParameter, "complexParameter should not be null");
        assertEquals("A complex parameter", complexParameter.getDescription(), "Description mismatch for complexParameter");
        assertEquals(2, complexParameter.getProperties().size(), "Expected exactly 2 properties in complexParameter");

        // Validate nestedString
        JsonStringSchema nestedStringParameter = (JsonStringSchema) complexParameter.getProperties().get("nestedString");
        assertNotNull(nestedStringParameter, "nestedString should not be null");
        assertEquals("A nested string", nestedStringParameter.getDescription(), "Description mismatch for nestedString");

        // Validate nestedNumber
        JsonNumberSchema nestedNumberParameter = (JsonNumberSchema) complexParameter.getProperties().get("nestedNumber");
        assertNotNull(nestedNumberParameter, "nestedNumber should not be null");
        assertEquals("A nested number", nestedNumberParameter.getDescription(), "Description mismatch for nestedNumber");
    }

    @Test
    void toolWithNoParams() throws JsonProcessingException {
        String text = "[{\n" +
                "                    \"name\" : \"getTinyImage\",\n" +
                "                    \"description\" : \"Returns the MCP_TINY_IMAGE\",\n" +
                "                    \"inputSchema\" : {\n" +
                "                        \"type\" : \"object\",\n" +
                "                        \"properties\" : { },\n" +
                "                        \"additionalProperties\" : false,\n" +
                "                        \"$schema\" : \"http://json-schema.org/draft-07/schema#\"\n" +
                "                    }\n" +
                "                }]";
        ArrayNode json = JsonUtils.fromJson(text, ArrayNode.class);
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
        JsonObjectSchema parameters = toolSpecification.getParameters();
        assertNotNull(parameters, "Parameters should not be null");
        assertTrue(parameters.getProperties().isEmpty(), "Expected parameters to be empty");
    }
}
