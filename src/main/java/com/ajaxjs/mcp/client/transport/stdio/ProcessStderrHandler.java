package com.ajaxjs.mcp.client.transport.stdio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

class ProcessStderrHandler implements Runnable {
    private final Process process;
    private static final Logger log = LoggerFactory.getLogger(ProcessStderrHandler.class);

    public ProcessStderrHandler(final Process process) {
        this.process = process;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[ERROR] {}", line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.debug("ProcessErrorPrinter has finished reading error output from process with PID = " + getPid(process));
    }

    public static long getPid(Process process) {
        try {
            // 获取 Process 类的实现类中的 "pid" 字段
            Field field = process.getClass().getDeclaredField("pid");
            field.setAccessible(true); // 设置为可访问
            return (long) field.get(process); // 获取 pid 值
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get PID", e);
        }
    }
}
