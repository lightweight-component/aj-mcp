package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents server store resource.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreResource extends ServerStoreBase {
    /**
     * Holds the resource value.
     */
    ResourceItem resource;
}
