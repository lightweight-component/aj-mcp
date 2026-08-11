package com.ajaxjs.mcp.server.feature.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Runtime binding for a prompt or resource-template completion provider.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreCompletion extends ServerStoreBase {
    private String referenceType;
    private String referenceName;
    private String argumentName;
}
