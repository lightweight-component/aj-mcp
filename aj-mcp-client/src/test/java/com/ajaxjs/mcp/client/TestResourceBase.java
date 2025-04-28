package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.resource.Resource;
import com.ajaxjs.mcp.client.protocol.resource.ResourceTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class TestResourceBase {
    static IMcpClient mcpClient;

    @Test
    public void resourceList() {
        List<Resource> resourceList = mcpClient.listResources();
        assertEquals(2, resourceList.size(), "Expected exactly two resources");

        Resource blob = getResourceRef("blob", resourceList);
        assertNotNull(blob, "Resource 'blob' should not be null");
        assertEquals("blob", blob.getName(), "Name mismatch for 'blob'");
        assertEquals("file:///blob", blob.getUri(), "URI mismatch for 'blob'");
        assertEquals("text/blob", blob.getMimeType(), "MIME type mismatch for 'blob'");
        assertEquals("A nice blob", blob.getDescription(), "Description mismatch for 'blob'");

        Resource text = getResourceRef("text", resourceList);
        assertNotNull(text, "Resource 'text' should not be null");
        assertEquals("text", text.getName(), "Name mismatch for 'text'");
        assertEquals("file:///text", text.getUri(), "URI mismatch for 'text'");
        assertEquals("text/plain", text.getMimeType(), "MIME type mismatch for 'text'");
        assertEquals("A nice piece of text", text.getDescription(), "Description mismatch for 'text'");
    }

    private Resource getResourceRef(String resourceName, List<Resource> resourceRefs) {
        for (Resource ref : resourceRefs) {
            if (ref.getName().equals(resourceName))
                return ref;
        }

        return null;
    }

    private ResourceTemplate getResourceTemplateRef(String resourceTemplateName, List<ResourceTemplate> resourceTemplateRefs) {
        for (ResourceTemplate ref : resourceTemplateRefs) {
            if (ref.getName().equals(resourceTemplateName))
                return ref;
        }

        return null;
    }
}
