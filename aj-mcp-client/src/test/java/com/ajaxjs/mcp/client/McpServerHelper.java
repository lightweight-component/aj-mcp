package com.ajaxjs.mcp.client;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
public class McpServerHelper {
    static Process startServerHttp() {
        skipTestsIfServerNotAvailable();
        String[] command = new String[]{TestToolStdio.getConfig().get("cmd_path"), "-transport", "sse"};
        log.info("Starting the MCP server using command: {}", Arrays.toString(command));
        Process process;

        try {
            process = new ProcessBuilder().command(command).inheritIO().start();
            waitForPort(8000, 12);
            log.info("MCP server has started");
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        return process;
    }

    static String getPathToScript(String script) {
        return ClassLoader.getSystemResource(script)
                .getFile()
                .substring(System.getProperty("os.name").toLowerCase().contains("windows") ? 1 : 0)
                .replace("/", File.separator);
    }

    static void skipTestsIfServerNotAvailable() {
        String command = TestToolStdio.getConfig().get("cmd_path");

        try {
            new ProcessBuilder().command(command, "--version").start().waitFor();
        } catch (Exception e) {
            String message = "gitee is not available (could not execute command '" + command + "', MCP integration tests will be skipped. ";
            log.warn(message, e);
            assumeTrue(false, message);
        }

        log.info("cmd ok");
    }

    private static void waitForPort(int port, int timeoutSeconds) throws InterruptedException, TimeoutException {
        Request request = new Request.Builder().url("http://localhost:" + port).build();
        long start = System.currentTimeMillis();
        OkHttpClient client = new OkHttpClient();

        while (System.currentTimeMillis() - start < timeoutSeconds * 1000L) {
            try {
                client.newCall(request).execute();
                return;
            } catch (IOException e) {
                TimeUnit.SECONDS.sleep(1);
            }
        }

        throw new TimeoutException("Port " + port + " did not open within " + timeoutSeconds + " seconds");
    }
}
