package com.ajaxjs.mcp.tool;

import lombok.Data;

@Data
public class ToolSpecification {
    private String name;

    private String description;
//TODO
//    private final JsonObjectSchema parameters;

    private ToolSpecification(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
//        this.parameters = builder.parameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String description;
//        private JsonObjectSchema parameters;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

//        public Builder parameters(JsonObjectSchema parameters) {
//            this.parameters = parameters;
//            return this;
//        }

        public ToolSpecification build() {
            return new ToolSpecification(this);
        }
    }
}
