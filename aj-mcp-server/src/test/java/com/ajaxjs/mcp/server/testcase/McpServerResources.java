package com.ajaxjs.mcp.server.testcase;

import com.ajaxjs.mcp.protocol.resource.ResourceContentBinary;
import com.ajaxjs.mcp.protocol.resource.ResourceContentText;
import com.ajaxjs.mcp.server.common.ServerUtils;
import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Resource;

@McpService
public class McpServerResources {
    @Resource(uri = "file:///blob", description = "A nice pic", mimeType = "image/jpg")
    public ResourceContentBinary blob() {
        String base64EncodedImage = ServerUtils.encodeImageToBase64("bird-eye.jpg");

        ResourceContentBinary content = new ResourceContentBinary();
        content.setUri("file:///blob");
        content.setMimeType("image/jpg");
        content.setBlob(base64EncodedImage);

        return content;
    }

    @Resource(uri = "file:///text", description = "A nice piece of text", mimeType = "text/plain")
    public ResourceContentText text() {
        ResourceContentText content = new ResourceContentText();
        content.setUri("file:///text");
        content.setMimeType("file:///text");
        content.setText("text888");

        return content;
    }


    // resource templates
//    @ResourceTemplate(uriTemplate = "file:///text-template/{message}")
//   public TextResourceContents text_template(String message, RequestUri uri) {
//        return TextResourceContents.create(uri.value(), "text " + message);
//    }
//
//    @ResourceTemplate(uriTemplate = "file:///blob-template/{message}")
//  public  BlobResourceContents blob_template(String message, RequestUri uri) {
//        return BlobResourceContents.create(uri.value(), "blob " + message);
//    }
}
