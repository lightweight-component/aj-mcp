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
     * Completion reference type, such as prompt or resource-template reference.
     */
    private String referenceType;
    /**
     * Name of the prompt or resource template that owns the completed argument.
     */
    private String referenceName;
    /**
     * Argument name for which this provider returns completion candidates.
     */
    private String argumentName;
}
