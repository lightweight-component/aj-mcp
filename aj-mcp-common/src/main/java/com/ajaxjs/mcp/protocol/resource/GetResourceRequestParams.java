package com.ajaxjs.mcp.protocol.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents get resource request params.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetResourceRequestParams extends com.ajaxjs.mcp.protocol.common.Metadata {
    /**
     * Holds the uri value.
     */
    private String uri;
}
