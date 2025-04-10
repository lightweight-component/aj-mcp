///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.20.0}@pom
//DEPS io.quarkiverse.mcp:quarkus-mcp-server-stdio:1.0.0.CR1
//DEPS io.quarkiverse.mcp:quarkus-mcp-server-sse:1.0.0.CR1

import io.quarkiverse.mcp.server.*;

import java.util.List;

public class prompts_mcp_server {

    @Prompt(description = "Basic simple prompt")
    PromptMessage basic() {
        return PromptMessage.withUserRole(new TextContent("Hello"));
    }

    @Prompt(description = "Prompt that returns two messages")
    List<PromptMessage> multi() {
        return List.of(
                PromptMessage.withUserRole(new TextContent("first")),
                PromptMessage.withUserRole(new TextContent("second"))
        );
    }

    @Prompt(description = "Parametrized prompt")
    PromptMessage parametrized(@PromptArg(description = "The name") String name) {
        return PromptMessage.withUserRole(new TextContent("Hello " + name));
    }

    @Prompt(description = "Prompt that returns an image")
    PromptMessage image() {
        return PromptMessage.withUserRole(new ImageContent("aaa", "image/png"));
    }

    @Prompt(description = "Prompt that returns an embedded binary resource")
    PromptMessage embeddedBinaryResource() {
        ResourceContents blob = new BlobResourceContents("file:///embedded-blob", "aaaaa", "application/octet-stream");
        Content content = new EmbeddedResource(blob);
        return new PromptMessage("user", content);
    }

}
