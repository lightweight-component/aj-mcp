package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.client.McpClient;
import com.ajaxjs.mcp2026.client.StdioClientTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the packaged STDIO example as an external MCP process.
 */
class PackagedStdioIT {
    @Test
    @Timeout(20)
    void runsExampleFromPackagedModuleJars() throws Exception {
        List<String> entries = new ArrayList<>();
        int packaged = 0;
        for (String entry : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            Path path = Paths.get(entry).toAbsolutePath().normalize();
            String normalized = path.toString().replace('\\', '/');
            if (normalized.matches(".*/mcp-(core|client|server)/target/classes")) {
                try (Stream<Path> files = Files.list(path.getParent())) {
                    path = files.filter(p -> p.getFileName().toString().matches("aj-mcp-next-.*(?<!-sources)\\.jar"))
                            .findFirst().orElseThrow(() -> new IllegalStateException("Run Maven verify after package"));
                }
            }
            if (path.getFileName().toString().matches("aj-mcp-next-(core|client|server)-[0-9].*(?<!-sources)\\.jar"))
                packaged++;
            entries.add(path.toString());
        }
        // Failsafe may already substitute all reactor artifacts in its classpath.
        assertEquals(3, packaged, "All three modules must run from packaged JARs");
        String executable = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
        try (McpClient client = new McpClient(StdioClientTransport.launch(Arrays.asList(executable, "-cp",
                String.join(File.pathSeparator, entries), "com.ajaxjs.mcp2026.server.example.EchoServer")))) {
            assertEquals("packaged", TestTransports.text(client.callTool("echo", Collections.singletonMap("text", "packaged"))));
        }
    }
}
