package com.ajaxjs.mcp.protocol.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents resource content text.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceContentText extends ResourceContent {
    /**
     * Holds the text value.
     */
    String text;
}
