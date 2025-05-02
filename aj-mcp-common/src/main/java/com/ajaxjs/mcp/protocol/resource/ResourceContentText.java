package com.ajaxjs.mcp.protocol.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceContentText extends ResourceContent {
    String text;
}
