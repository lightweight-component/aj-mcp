package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.resource.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TestResourceBase {
    static IMcpClient mcpClient;

    @Test
    public void resourceList() {
        List<ResourceItem> resourceList = mcpClient.listResources();
        assertEquals(2, resourceList.size(), "Expected exactly two resources");

        ResourceItem blob = getResourceRef("blob", resourceList);
        assertNotNull(blob, "Resource 'blob' should not be null");
        assertEquals("blob", blob.getName(), "Name mismatch for 'blob'");
        assertEquals("file:///blob", blob.getUri(), "URI mismatch for 'blob'");
        assertEquals("image/jpg", blob.getMimeType(), "MIME type mismatch for 'blob'");
        assertEquals("A nice pic", blob.getDescription(), "Description mismatch for 'blob'");

        ResourceItem text = getResourceRef("text", resourceList);
        assertNotNull(text, "Resource 'text' should not be null");
        assertEquals("text", text.getName(), "Name mismatch for 'text'");
        assertEquals("file:///text", text.getUri(), "URI mismatch for 'text'");
        assertEquals("text/plain", text.getMimeType(), "MIME type mismatch for 'text'");
        assertEquals("A nice piece of text", text.getDescription(), "Description mismatch for 'text'");
    }

    @Test
    public void readTextResource() {
        GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///text");
        assertEquals(1, response.getContents().size(), "Expected exactly one content");

        ResourceContent contents = response.getContents().get(0);

        ResourceContentText textContents = (ResourceContentText) contents;
        assertEquals("file:///text", textContents.getUri(), "URI should be 'file:///text'");
        assertEquals("text888", textContents.getText(), "Text content should be 'text'");
    }

    @Test
    public void readBlobResource() {
        GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///blob");
        assertEquals(1, response.getContents().size(), "Expected exactly one content");

        ResourceContent contents = response.getContents().get(0);

        ResourceContentBinary blobContents = (ResourceContentBinary) contents;
        assertEquals("file:///blob", blobContents.getUri(), "URI should be 'file:///blob'");
        assertEquals("blob", blobContents.getBlob(), "Blob content should be 'blob'");
    }

    @Test
    public void readTextResourceFromTemplate() {
        GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///text-template/hello");
        assertEquals(1, response.getContents().size(), "Expected exactly one content");

        ResourceContent contents = response.getContents().get(0);

        ResourceContentText textContents = (ResourceContentText) contents;
        assertEquals("file:///text-template/hello", textContents.getUri(), "URI should be 'file:///text-template/hello'");
        assertEquals("text hello", textContents.getText(), "Text content should be 'text hello'");
    }

    @Test
    public void readBlobResourceFromTemplate() {
        GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///blob-template/hello");
        assertEquals(1, response.getContents().size(), "Expected exactly one content");

        ResourceContent contents = response.getContents().get(0);

        ResourceContentBinary blobContents = (ResourceContentBinary) contents;
        assertEquals("file:///blob-template/hello", blobContents.getUri(), "URI should be 'file:///blob-template/hello'");
        assertEquals("blob hello", blobContents.getBlob(), "Blob content should be 'blob hello'");
    }

    @Test
    public void readNonExistentResource() {
        assertThrows(McpException.class, () -> mcpClient.readResource("file:///i-do-not-exist"), "Expected McpException to be thrown for non-existent resource");
    }


    private ResourceItem getResourceRef(String resourceName, List<ResourceItem> resourceRefs) {
        for (ResourceItem ref : resourceRefs) {
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
