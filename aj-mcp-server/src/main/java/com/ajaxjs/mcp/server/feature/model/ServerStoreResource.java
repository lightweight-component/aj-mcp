package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Runtime binding for one concrete MCP resource.
 * <p>
 * The resource metadata identifies the URI advertised to clients; the inherited Java method
 * binding produces the resource contents when that URI is read.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreResource extends ServerStoreBase {
    /**
     * Resource metadata returned by {@code resources/list}.
     */
    ResourceItem resource;
}
