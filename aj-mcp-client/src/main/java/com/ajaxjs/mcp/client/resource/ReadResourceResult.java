package com.ajaxjs.mcp.client.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReadResourceResult {
    List<ResourceContents> contents;
}
