package com.ajaxjs.mcp.protocol;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.common.Content;
import com.ajaxjs.mcp.protocol.common.ContentResourceLink;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.ajaxjs.mcp.protocol.initialize.InitializeResponseResult;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.tools.CallToolResultDetail;
import com.ajaxjs.mcp.protocol.utils.RequestMeta;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MetadataRoundTripTest {
    @Test
    void contentAndResourceHintsSurviveConversion() {
        String json = "{\"type\":\"resource_link\",\"uri\":\"demo://a\",\"name\":\"a\",\"title\":\"Display A\","
                + "\"annotations\":{\"audience\":[\"assistant\"],\"priority\":0.5,\"lastModified\":\"2025-06-18T12:00:00Z\"},"
                + "\"_meta\":{\"vendor/key\":{\"items\":[1,true,\"text\"]}}}";
        JsonNode expected = JsonUtils.json2Node(json);
        Content result = JsonUtils.convertValue(expected, Content.class);
        assertTrue(result instanceof ContentResourceLink);
        assertEquals(expected, JsonUtils.valueToTree(result));
        ResourceItem resource = JsonUtils.convertValue(expected, ResourceItem.class);
        assertEquals(expected.get("_meta"), JsonUtils.valueToTree(resource).get("_meta"));
        assertEquals(expected.get("annotations"), JsonUtils.valueToTree(resource).get("annotations"));
        assertFalse(JsonUtils.valueToTree(new ContentText("plain")).has("_meta"));
        assertFalse(JsonUtils.valueToTree(new ContentText("plain")).has("annotations"));
    }

    @Test
    void resultMetadataIsNestedAndRequestExtensionsPreserveProgressApi() {
        CallToolResultDetail result = new CallToolResultDetail();
        result.setMeta(Collections.singletonMap("vendor/key", Collections.singletonList("value")));
        JsonNode wire = JsonUtils.valueToTree(result);
        assertTrue(wire.has("_meta"));
        assertFalse(wire.has("meta"));
        assertEquals(result.getMeta(), JsonUtils.convertValue(wire, CallToolResultDetail.class).getMeta());
        RequestMeta meta = new RequestMeta("progress-1");
        meta.setExtension("vendor/key", Collections.singletonMap("enabled", true));
        JsonNode requestWire = JsonUtils.valueToTree(meta);
        assertFalse(requestWire.has("extensions"));
        assertEquals("progress-1", requestWire.path("progressToken").asText());
        assertEquals(requestWire, JsonUtils.valueToTree(JsonUtils.convertValue(requestWire, RequestMeta.class)));
    }

    @Test
    void implementationTitlesRoundTrip() {
        InitializeRequestParams.ClientInfo client = new InitializeRequestParams.ClientInfo();
        client.setName("client");
        client.setVersion("1");
        client.setTitle("Client label");
        assertEquals("Client label", JsonUtils.convertValue(JsonUtils.valueToTree(client),
                InitializeRequestParams.ClientInfo.class).getTitle());
        InitializeResponseResult.ServerInfo server = new InitializeResponseResult.ServerInfo();
        server.setTitle("Server label");
        assertEquals("Server label", JsonUtils.valueToTree(server).path("title").asText());
        com.ajaxjs.mcp.protocol.prompt.PromptArgument argument = JsonUtils.convertValue(
                JsonUtils.json2Node("{\"name\":\"topic\",\"title\":\"Topic label\"}"),
                com.ajaxjs.mcp.protocol.prompt.PromptArgument.class);
        assertEquals("Topic label", argument.getTitle());
        com.ajaxjs.mcp.protocol.utils.completion.CompleteRequest.ParamsRef ref =
                new com.ajaxjs.mcp.protocol.utils.completion.CompleteRequest.ParamsRef("ref/prompt", "explain", null);
        ref.setTitle("Explain label");
        assertEquals("Explain label", JsonUtils.valueToTree(ref).path("title").asText());
    }
}
