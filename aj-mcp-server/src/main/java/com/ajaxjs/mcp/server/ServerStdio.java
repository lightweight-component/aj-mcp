package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRaw;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
public class ServerStdio implements McpTransportSync {
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    private final PrintWriter writer = new PrintWriter(System.out, true);

    private final AtomicBoolean running = new AtomicBoolean(true);

    private McpServer server;

    public ServerStdio(McpServer server) {
        this.server = server;
    }

    @Override
    public void start() {
        // 启动输入处理线程
        new Thread(this::processInput).start();

        while (running.get()) { // 主线程处理其他任务
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void processInput() {
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                try {
                    McpRequestRaw request = McpServerInitialize.jsonRpcValidate(line); // 解析输入消息
                    String response = JsonUtils.toJson(server.processMessage(request));  // 处理消息并生成响应
                    writer.println(response);    // 发送响应
                    writer.flush();
                } catch (JsonRpcErrorException e) {
                    writer.println(e.toJson());
                    writer.flush();
                } catch (Exception e) {
                    log.warn("消息处理错误: {}", e.getMessage());
                    JsonRpcErrorException jsonErr = new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, e.getMessage());
                    writer.println(jsonErr.toJson());
                    writer.flush();
                }
            }
        } catch (IOException e) {
            log.warn("输入处理错误: {}", e.getMessage());
        } finally {
            running.set(false);
        }
    }

    @Override
    public void handle(JsonNode message) {

    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() throws IOException {

    }
}
