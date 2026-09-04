package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.McpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Represents stdio transport close test.
 */
class StdioTransportCloseTest {
    @Test
    @Timeout(10)
    void closeStopsProcessAndReaderThreads() throws Exception {
        StdioTransport transport = StdioTransport.builder()
                .command(Arrays.asList(
                        javaExecutable(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        PersistentFakeMcpServer.class.getName()))
                .build();
        McpClient client = McpClient.builder().transport(transport).build();

        client.initialize();

        Thread stdoutThread = field(transport, "stdoutReaderThread", Thread.class);
        Thread stderrThread = field(transport, "stderrReaderThread", Thread.class);
        Process process = field(transport, "process", Process.class);
        assertTrue(stdoutThread.isDaemon());
        assertTrue(stderrThread.isDaemon());
        assertTrue(process.isAlive());

        client.close();

        assertFalse(process.isAlive());
        assertFalse(stdoutThread.isAlive());
        assertFalse(stderrThread.isAlive());
        assertDoesNotThrow(client::close);
    }

    private static String javaExecutable() {
        return System.getProperty("java.home") + java.io.File.separator + "bin"
                + java.io.File.separator + "java";
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    /**
     * Represents persistent fake mcp server.
     */
    public static class PersistentFakeMcpServer {
        public static void main(String[] args) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            if (reader.readLine() != null) {
                System.out.println("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{"
                        + "\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},"
                        + "\"serverInfo\":{\"name\":\"close-test\",\"version\":\"1.0\"}}}");
                System.out.flush();
            }

            Thread.sleep(60_000);
        }
    }
}
