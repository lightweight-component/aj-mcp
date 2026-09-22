package com.ajaxjs.mcp.protocol;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaRoundTripTest {
    @Test
    void preservesNestedSchemasAndConstraints() {
        JsonNode input = JsonUtils.json2Node("{\"type\":\"object\",\"properties\":{"
                + "\"disabled\":false,\"anything\":true,"
                + "\"values\":{\"type\":\"array\",\"items\":{\"type\":\"integer\",\"minimum\":0}},"
                + "\"choice\":{\"type\":[\"string\",\"null\"],\"enum\":[\"a\",null]},"
                + "\"nested\":{\"type\":\"object\",\"properties\":{\"x\":{\"maxLength\":8}}}},"
                + "\"additionalProperties\":{\"type\":\"string\"},\"required\":[\"values\"],"
                + "\"oneOf\":[{\"required\":[\"choice\"]}],\"$defs\":{\"x\":false},\"x-vendor\":{\"enabled\":true}}");
        JsonSchema schema = JsonUtils.fromJson(input.toString(), JsonSchema.class);
        assertEquals(input, JsonUtils.json2Node(JsonUtils.toJson(schema)));
        schema.setAdditionalProperties(false);
        schema.getProperties().get("choice").setType("string");
        JsonNode modified = JsonUtils.json2Node(JsonUtils.toJson(schema));
        assertFalse(modified.get("additionalProperties").asBoolean());
        assertEquals("string", modified.path("properties").path("choice").path("type").asText());
        assertTrue(modified.has("oneOf"));
    }
}
