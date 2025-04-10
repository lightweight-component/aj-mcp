///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.20.0}@pom
//DEPS io.quarkiverse.mcp:quarkus-mcp-server-stdio:1.0.0.CR1
//DEPS io.quarkiverse.mcp:quarkus-mcp-server-sse:1.0.0.CR1

import io.quarkiverse.mcp.server.*;

public class resources_mcp_server {

    // direct resources

    @Resource(uri = "file:///blob", description = "A nice blob", mimeType = "text/blob")
    BlobResourceContents blob() {
        return BlobResourceContents.create("file:///blob", "blob");
    }

    @Resource(uri = "file:///text", description = "A nice piece of text", mimeType = "text/plain")
    TextResourceContents text() {
        return TextResourceContents.create("file:///text", "text888");
    }

    // resource templates

    @ResourceTemplate(uriTemplate = "file:///text-template/{message}")
    TextResourceContents text_template(String message, RequestUri uri) {
        return TextResourceContents.create(uri.value(), "text " + message);
    }

    @ResourceTemplate(uriTemplate = "file:///blob-template/{message}")
    BlobResourceContents blob_template(String message, RequestUri uri) {
        return BlobResourceContents.create(uri.value(), "blob " + message);
    }

}
