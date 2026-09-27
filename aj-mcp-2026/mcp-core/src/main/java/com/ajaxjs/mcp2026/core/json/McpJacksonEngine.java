package com.ajaxjs.mcp2026.core.json;

import com.ajaxjs.util.json.jackson2.Jackson2Engine;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.IOException;

/**
 * Reuses AJ Util's engine and mapper. Only inbound parsing is specialized:
 * exact numbers, bounded nesting, one JSON document and no payload logging.
 * Configure before sharing; do not mutate the inherited mapper during use.
 */
public final class McpJacksonEngine extends Jackson2Engine {
    /**
     * Configures strict inbound JSON parsing and resource limits before the engine is shared.
     */
    public McpJacksonEngine() {
        OBJECT_MAPPER.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        OBJECT_MAPPER.disable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);
        OBJECT_MAPPER.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        OBJECT_MAPPER.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        OBJECT_MAPPER.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        OBJECT_MAPPER.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(64).maxNumberLength(1000).maxStringLength(4 * 1024 * 1024).build());
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            // Retain diagnostic causes without sending them or raw payloads to logs/the wire.
            throw new IllegalArgumentException("Invalid JSON document", e);
        }
    }
}
