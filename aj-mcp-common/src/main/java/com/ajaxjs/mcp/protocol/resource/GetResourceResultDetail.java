package com.ajaxjs.mcp.protocol.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GetResourceResultDetail {
    List<ResourceContent> contents;
}