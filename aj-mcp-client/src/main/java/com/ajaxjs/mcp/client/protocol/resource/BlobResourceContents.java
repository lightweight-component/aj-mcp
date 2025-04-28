package com.ajaxjs.mcp.client.protocol.resource;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Base64;

@Data
@AllArgsConstructor
public class BlobResourceContents implements ResourceContents {
    String uri;
    String blob;
    String mimeType;

    public BlobResourceContents(String uri, String blob) {
        this.uri = uri;
        this.blob = blob;
    }

    public BlobResourceContents(String uri, byte[] blob) {
        this(uri, Base64.getMimeEncoder().encodeToString(blob));
    }

    @Override
    public Type getType() {
        return Type.BLOB;
    }
}
