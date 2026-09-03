package com.ajaxjs.mcp.protocol.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetResourceRequestParams {
    String uri;
}