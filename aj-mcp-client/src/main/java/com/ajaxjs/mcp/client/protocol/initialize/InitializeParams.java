package com.ajaxjs.mcp.client.protocol.initialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class InitializeParams {
    private String protocolVersion;

    private Capabilities capabilities;

    private ClientInfo clientInfo;

    @Data
    public static class Capabilities {
        private Roots roots;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Sampling sampling;

        public static class Roots {
            private boolean listChanged;

            public boolean isListChanged() {
                return listChanged;
            }

            public void setListChanged(final boolean listChanged) {
                this.listChanged = listChanged;
            }
        }

        public static class Sampling {
        }
    }

    @Data
    public static class ClientInfo {
        private String name;

        private String version;
    }
}
