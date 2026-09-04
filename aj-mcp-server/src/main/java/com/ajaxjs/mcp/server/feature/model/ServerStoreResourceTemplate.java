package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Runtime binding between an RFC 6570 level-1 template and its Java method.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreResourceTemplate extends ServerStoreBase {
    /**
     * Holds the resource template value.
     */
    private ResourceTemplate resourceTemplate;
    /**
     * Holds the parameter names value.
     */
    private List<String> parameterNames;
    /**
     * Holds the template variable names value.
     */
    private List<String> templateVariableNames;
    /**
     * Holds the uri pattern value.
     */
    private Pattern uriPattern;
}
