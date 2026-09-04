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
public class GetResourceRequestParams {
    /**
     * Holds the uri value.
     */
    String uri;
}