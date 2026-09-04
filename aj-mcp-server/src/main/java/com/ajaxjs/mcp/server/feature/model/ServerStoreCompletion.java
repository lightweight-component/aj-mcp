package com.ajaxjs.mcp.server.feature.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Runtime binding for a prompt or resource-template completion provider.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreCompletion extends ServerStoreBase {
    /**
     * Holds the reference type value.
     */
    private String referenceType;
    /**
     * Holds the reference name value.
     */
    private String referenceName;
    /**
     * Holds the argument name value.
     */
    private String argumentName;
}
