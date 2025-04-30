package com.ajaxjs.mcp.server.common;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

public class ServerUtils {
    public static String encodeImageToBase64(String resourcePath) {
        try (InputStream inputStream = ServerUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null)
                throw new IllegalArgumentException("Resource not found: " + resourcePath);

            // 将 InputStream 转换为 ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1)
                outputStream.write(buffer, 0, bytesRead);

            // 获取字节数组
            byte[] imageBytes = outputStream.toByteArray();

            // Base64 编码
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode image to Base64", e);
        }
    }
}
