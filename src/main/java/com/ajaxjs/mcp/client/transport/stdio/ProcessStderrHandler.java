package com.ajaxjs.mcp.client.transport.stdio;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

/**
 * Process for the Error output
 */
@Slf4j
public class ProcessStderrHandler implements Runnable {
    private final Process process;

    public ProcessStderrHandler(Process process) {
        this.process = process;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.warn("[ERROR] {}", line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        log.info("ProcessErrorPrinter has finished reading error output from process");
    }
}
