package com.ajaxjs.mcp.resource;

import com.ajaxjs.mcp.IllegalResponseException;
import com.ajaxjs.mcp.McpException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ResourcesHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<Resource> parseResourceRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");

            if (resultNode.has("resources")) {
                List<Resource> resourceRefs = new ArrayList<>();

                for (JsonNode resourceNode : resultNode.get("resources"))
                    resourceRefs.add(OBJECT_MAPPER.convertValue(resourceNode, Resource.class));

                return resourceRefs;
            } else {
                log.warn("Result does not contain 'resources' element: {}", resultNode);
                throw new IllegalResponseException("Result does not contain 'resources' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }

    public static ReadResourceResult parseResourceContents(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");
            if (resultNode.has("contents")) {
                List<ResourceContents> resourceContentsList = new ArrayList<>();

                for (JsonNode resourceNode : resultNode.get("contents")) {
                    String uri = resourceNode.get("uri").asText();
                    String mimeType = resourceNode.get("mimeType") != null ? resourceNode.get("mimeType").asText() : null;

                    if (resourceNode.has("text")) {
                        resourceContentsList.add(new TextResourceContents(uri, resourceNode.get("text").asText(), mimeType));
                    } else if (resourceNode.has("blob")) {
                        resourceContentsList.add(new BlobResourceContents(uri, resourceNode.get("blob").asText(), mimeType));
                    }
                }

                return new ReadResourceResult(resourceContentsList);
            } else {
                log.warn("Result does not contain 'contents' element: {}", resultNode);
                throw new IllegalResponseException("Result does not contain 'resources' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }

  public  static List<ResourceTemplate> parseResourceTemplateRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");

            if (resultNode.has("resourceTemplates")) {
                List<ResourceTemplate> resourceTemplateRefs = new ArrayList<>();

                for (JsonNode resourceTemplateNode : resultNode.get("resourceTemplates"))
                    resourceTemplateRefs.add(OBJECT_MAPPER.convertValue(resourceTemplateNode, ResourceTemplate.class));

                return resourceTemplateRefs;
            } else {
                log.warn("Result does not contain 'resourceTemplates' element: {}", resultNode);
                throw new IllegalResponseException("Result does not contain 'resourceTemplates' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }
}
