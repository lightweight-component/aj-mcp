package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,      // 使用名称来区分子类
        include = JsonTypeInfo.As.PROPERTY, // 使用 JSON 属性作为类型标识
        property = "type"               // 指定用来标识类型的字段
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PromptContentText.class, name = "text"),
        @JsonSubTypes.Type(value = PromptContentImage.class, name = "image"),
        @JsonSubTypes.Type(value = PromptContentAudio.class, name = "audio"),
        @JsonSubTypes.Type(value = PromptContentEmbeddedResource.class, name = "resource")
})
public abstract class PromptContent {
    String type;
}
