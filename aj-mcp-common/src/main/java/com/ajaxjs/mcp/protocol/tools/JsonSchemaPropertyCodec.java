package com.ajaxjs.mcp.protocol.tools;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/** Property schemas may be objects or booleans; preserve both without coercion. */
public final class JsonSchemaPropertyCodec {
    private JsonSchemaPropertyCodec() { }

    public static final class Reader extends JsonDeserializer<JsonSchemaProperty> {
        @Override
        public JsonSchemaProperty deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            JsonSchemaProperty result = new JsonSchemaProperty();
            if (node.isBoolean()) {
                result.setBooleanSchema(node.booleanValue());
                return result;
            }
            if (!node.isObject())
                return (JsonSchemaProperty) context.handleUnexpectedToken(JsonSchemaProperty.class, parser);
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if ("type".equals(field.getKey()))
                    result.setTypeValue(field.getValue());
                else if ("description".equals(field.getKey()) && field.getValue().isTextual())
                    result.setDescription(field.getValue().textValue());
                else
                    result.setKeyword(field.getKey(), field.getValue());
            }
            return result;
        }
    }

    public static final class Writer extends JsonSerializer<JsonSchemaProperty> {
        @Override
        public void serialize(JsonSchemaProperty value, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            if (value.getBooleanSchema() != null) {
                generator.writeBoolean(value.getBooleanSchema());
                return;
            }
            generator.writeStartObject();
            if (value.getTypeValue() != null)
                generator.writeObjectField("type", value.getTypeValue());
            if (value.getDescription() != null)
                generator.writeStringField("description", value.getDescription());
            for (Map.Entry<String, JsonNode> entry : value.getKeywords().entrySet()) {
                generator.writeFieldName(entry.getKey());
                generator.writeTree(entry.getValue());
            }
            generator.writeEndObject();
        }
    }
}
