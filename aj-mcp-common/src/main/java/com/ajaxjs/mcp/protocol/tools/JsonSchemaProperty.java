package com.ajaxjs.mcp.protocol.tools;

import lombok.Data;

//import java.util.Map;

/**
 * The properties of Json Schema are actually parameters.
 */
@Data
public class JsonSchemaProperty {
    /**
     * Holds the type value.
     */
    private String type;

    /**
     * Holds the description value.
     */
    private String description;

// for complexParameter
//    private Map<String, JsonSchemaProperty> properties;

}
