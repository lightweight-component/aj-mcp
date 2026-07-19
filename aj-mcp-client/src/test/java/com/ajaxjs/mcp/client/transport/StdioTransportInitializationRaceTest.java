package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.McpClient;
import org.junit.jupiter.api.RepeatedTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StdioTransportInitializationRaceTest {
    private static final String INITIALIZE_RESPONSE =
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{"
                    + "\"protocolVersion\":\"2024-11-05\","
                    + "\"capabilities\":{},"
                    + "\"serverInfo\":{\"name\":\"race-test\",\"version\":\"1.0\"}}}";

    /**
     * Stresses the contract that start() must make stdin writable before it returns.
     * <p>
     * The current implementation creates the PrintStream in a background thread, so
     * initialize() can reach out.println(...) while out is still null.
     */
    @RepeatedTest(1000)
    void initializeImmediatelyAfterStartMustNotRaceWithOutputStreamCreation() {
        assertDoesNotThrow(() -> {
            StdioTransport transport = StdioTransport.builder()
                    .command(Arrays.asList(
                            javaExecutable(),
                            "-cp",
                            System.getProperty("java.class.path"),
                            FakeMcpServer.class.getName()))
                    .build();

            McpClient client = McpClient.builder().transport(transport).build();
            try {
                client.initialize();
            } finally {
                client.close();
            }
        });
    }

    private static String javaExecutable() {
        return System.getProperty("java.home") + java.io.File.separator + "bin"
                + java.io.File.separator + "java";
    }

    public static class FakeMcpServer {
        public static void main(String[] args) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            if (reader.readLine() != null) {
                System.out.println(INITIALIZE_RESPONSE);
                System.out.flush();
            }

            // Consume notifications/initialized so the client can close normally.
            reader.readLine();
        }
    }
}
