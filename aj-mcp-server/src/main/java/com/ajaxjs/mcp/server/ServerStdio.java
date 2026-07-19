package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.transport.McpTransportSync;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
public class ServerStdio implements McpTransportSync {
    private final InputStream input = System.in;

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

    private final PrintWriter writer = new PrintWriter(System.out, true);

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final Object lifecycleLock = new Object();

    private volatile Thread inputThread;

    private McpServer server;

    public ServerStdio(McpServer server) {
        this.server = server;
    }

    @Override
    public void start() {
        synchronized (lifecycleLock) {
            if (closed.get())
                throw new IllegalStateException("STDIO server transport is closed");

            if (!started.compareAndSet(false, true))
                throw new IllegalStateException("STDIO server transport is already started");

            running.set(true);
            inputThread = new Thread(this::processInput, "aj-mcp-server-stdio-input");
            inputThread.setDaemon(true);
            inputThread.start();
        }

        try {
            while (running.get() && inputThread.isAlive())
                inputThread.join(250);
        } catch (InterruptedException e) {
            try {
                close();
            } catch (IOException closeError) {
                log.warn("Failed to close STDIO server transport after interruption", closeError);
            }
            Thread.currentThread().interrupt();
        }
    }

    private void processInput() {
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                try {
//                    McpRequestRawInfo request = McpServerInitialize.jsonRpcValidate(line); // 解析输入消息
//                    String response = JsonUtils.toJson(server.processMessage(request));  // 处理消息并生成响应
                    String response = handle(line);
                    if (response != null) {
                        writer.println(response);    // 发送响应
                        writer.flush();
                    }
                } catch (JsonRpcErrorException e) {
                    writer.println(e.toJson());
                    writer.flush();
                } catch (Exception e) {
                    log.warn("消息处理错误: {}", e.getMessage());
                    JsonRpcErrorException jsonErr = new JsonRpcErrorException(JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
                    writer.println(jsonErr.toJson());
                    writer.flush();
                }
            }
        } catch (IOException e) {
            if (!closed.get())
                log.warn("输入处理错误: {}", e.getMessage());
        } finally {
            running.set(false);
        }
    }

    @Override
    public String handle(String rawJson) {
        McpRequestRawInfo request = McpServerInitialize.jsonRpcValidate(rawJson); // 解析输入消息
        McpResponse mcpResponse = server.processMessage(request);

        return mcpResponse == null ? null : JsonUtils.toJson(mcpResponse);  // 处理消息并生成响应
    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true))
            return;

        running.set(false);
        IOException closeFailure = null;

        try {
            // BufferedReader.readLine() holds the reader lock while blocking. Closing the
            // underlying stream directly is therefore required to unblock the input thread.
            input.close();
        } catch (IOException e) {
            closeFailure = e;
        }

        writer.flush();

        Thread thread;
        synchronized (lifecycleLock) {
            thread = inputThread;
        }

        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
            boolean interrupted = false;

            try {
                thread.join(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                interrupted = true;
            }

            if (thread.isAlive())
                log.warn("STDIO server input thread did not stop within 2 seconds");

            if (interrupted)
                Thread.currentThread().interrupt();
        }

        try {
            reader.close();
        } catch (IOException e) {
            if (closeFailure == null)
                closeFailure = e;
            else
                closeFailure.addSuppressed(e);
        }

        if (closeFailure != null)
            throw closeFailure;
    }
}
