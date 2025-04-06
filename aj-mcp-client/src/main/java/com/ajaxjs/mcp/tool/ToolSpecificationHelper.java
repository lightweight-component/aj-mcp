package com.ajaxjs.mcp.tool;

import com.ajaxjs.mcp.client.json.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class ToolSpecificationHelper {
    /**
     * Converts the 'tools' element from a ListToolsResult MCP message to a list of ToolSpecification objects.
     */
    public static List<ToolSpecification> toolSpecificationListFromMcpResponse(ArrayNode array) {
        List<ToolSpecification> result = new ArrayList<>();

        for (JsonNode tool : array) {
            ToolSpecification toolSpecification = new ToolSpecification();
            toolSpecification.setName(tool.get("name").asText());

            if (tool.has("description"))
                toolSpecification.setDescription(tool.get("description").asText());

            toolSpecification.setParameters((JsonObjectSchema) jsonNodeToJsonSchemaElement(tool.get("inputSchema")));
            result.add(toolSpecification);
        }

        return result;
    }

    /**
     * Converts the 'inputSchema' element (inside the 'Tool' type in the MCP schema)
     * to a JsonSchemaElement object that describes the tool's arguments.
     */
    public static JsonSchemaElement jsonNodeToJsonSchemaElement(JsonNode node) {
        String nodeType = node.get("type").asText();

        if (nodeType.equals("object")) {
            JsonObjectSchema.JsonObjectSchemaBuilder builder = JsonObjectSchema.builder();
            JsonNode required = node.get("required");
//            if (required != null)
//                builder.required(toStringArray((ArrayNode) required));

            if (node.has("additionalProperties"))
                builder.additionalProperties(node.get("additionalProperties").asBoolean(false));

            JsonNode description = node.get("description");
            if (description != null)
                builder.description(description.asText());

            JsonNode properties = node.get("properties");
            if (properties != null) {
                ObjectNode propertiesObject = (ObjectNode) properties;
//                for (Map.Entry<String, JsonNode> property : propertiesObject.properties())
//                    builder.addProperty(property.getKey(), jsonNodeToJsonSchemaElement(property.getValue()));
            }

            return builder.build();
        } else if (nodeType.equals("string")) {
            if (node.has("enum")) {
                JsonEnumSchema.JsonEnumSchemaBuilder builder = JsonEnumSchema.builder();
                if (node.has("description"))
                    builder.description(node.get("description").asText());

//                builder.enumValues(toStringArray((ArrayNode) node.get("enum")));
                return builder.build();
            } else {
                JsonStringSchema.JsonStringSchemaBuilder builder = JsonStringSchema.builder();
                if (node.has("description"))
                    builder.description(node.get("description").asText());

                return builder.build();
            }
        } else if (nodeType.equals("number")) {
            JsonNumberSchema.JsonNumberSchemaBuilder builder = JsonNumberSchema.builder();
            if (node.has("description"))
                builder.description(node.get("description").asText());

            return builder.build();
        } else if (nodeType.equals("integer")) {
            JsonIntegerSchema.JsonIntegerSchemaBuilder builder = JsonIntegerSchema.builder();
            if (node.has("description"))
                builder.description(node.get("description").asText());

            return builder.build();
        } else if (nodeType.equals("boolean")) {
            JsonBooleanSchema.JsonBooleanSchemaBuilder builder = JsonBooleanSchema.builder();
            if (node.has("description"))
                builder.description(node.get("description").asText());

            return builder.build();
        } else if (nodeType.equals("array")) {
            JsonArraySchema.JsonArraySchemaBuilder builder = JsonArraySchema.builder();
            if (node.has("description"))
                builder.description(node.get("description").asText());

            builder.items(jsonNodeToJsonSchemaElement(node.get("items")));
            return builder.build();
        } else
            throw new IllegalArgumentException("Unknown element type: " + nodeType);
    }

    private static String[] toStringArray(ArrayNode jsonArray) {
        String[] result = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++)
            result[i] = jsonArray.get(i).asText();

        return result;
    }
}
