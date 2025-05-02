package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.McpConstant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,      // 使用名称来区分子类
        include = JsonTypeInfo.As.PROPERTY, // 使用 JSON 属性作为类型标识
        property = "type"               // 指定用来标识类型的字段
)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = ContentText.class, name = McpConstant.ContentType.TEXT),
        @JsonSubTypes.Type(value = ContentImage.class, name = McpConstant.ContentType.IMAGE),
        @JsonSubTypes.Type(value = ContentAudio.class, name = McpConstant.ContentType.AUDIO),
        @JsonSubTypes.Type(value = ContentEmbeddedResource.class, name = McpConstant.ContentType.RESOURCE)
})
public abstract class Content {
    @JsonIgnore
    String type;
}
