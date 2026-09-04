package com.ajaxjs.mcp.protocol.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Represents get resource result detail.
 */
@Data
@AllArgsConstructor
public class GetResourceResultDetail {
    /**
     * Holds the contents value.
     */
    List<ResourceContent> contents;
}