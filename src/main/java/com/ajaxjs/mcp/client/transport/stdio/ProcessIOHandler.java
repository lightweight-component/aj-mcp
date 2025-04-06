package com.ajaxjs.mcp.client.transport.stdio;

import com.ajaxjs.mcp.client.transport.McpOperationHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

@Slf4j
public class ProcessIOHandler implements Runnable {
    private final Process process;
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final boolean logEvents;
    private final McpOperationHandler messageHandler;
    private final PrintStream out;

    public ProcessIOHandler(Process process, McpOperationHandler messageHandler, boolean logEvents) {
        this.process = process;
        this.logEvents = logEvents;
        this.messageHandler = messageHandler;
        this.out = new PrintStream(process.getOutputStream(), true);
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;

            while ((line = reader.readLine()) != null) {
//                if (logEvents)
//                    log.info("< {}", line);

                messageHandler.handle(OBJECT_MAPPER.readTree(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("ProcessIOHandler has finished reading output from process");
    }

    public void submit(String message) throws IOException {
        if (logEvents)
            log.debug("> {}", message);

        out.println(message);
    }
}
