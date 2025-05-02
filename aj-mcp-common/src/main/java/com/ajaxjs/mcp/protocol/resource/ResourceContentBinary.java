package com.ajaxjs.mcp.protocol.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceContentBinary extends ResourceContent {
    /**
     * base64-encoded-data
     */
    String blob;
}
