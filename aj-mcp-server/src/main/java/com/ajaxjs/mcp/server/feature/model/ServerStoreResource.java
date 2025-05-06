package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreResource extends ServerStoreBase {
    ResourceItem resource;
}
