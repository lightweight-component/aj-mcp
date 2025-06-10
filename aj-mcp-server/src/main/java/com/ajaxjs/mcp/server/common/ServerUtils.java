package com.ajaxjs.mcp.server.common;

import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.server.McpServerInitialize;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Server Common Utils
 */
public class ServerUtils {
    /**
     * 将指定路径的图像资源转换为 Base64 编码字符串
     * 此方法主要用于将存储在资源路径中的图像转换为 Base64 编码，以便在不需要直接访问文件系统的情况下传输图像数据
     *
     * @param resourcePath 图像资源的路径
     * @return Base64编码的图像字符串
     * @throws IllegalArgumentException 如果指定路径的资源未找到
     * @throws RuntimeException         如果图像处理或 Base64 编码过程中发生错误
     */
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


    public static <T> PaginatedResponse<T> paginate(List<T> list, Cursor params, McpServerInitialize instance) {
        Integer pageNo = params.getPageNo();
        Integer pageSize = instance.getServerConfig().getPageSize();

        return paginate(list, pageNo, pageSize);
    }

    /**
     * Paginates a list based on the given page number and page size.
     *
     * @param list     The list to paginate.
     * @param pageNo   The page number (1-based).
     * @param pageSize The number of items per page.
     * @return A PaginatedResponse containing the paginated list, a boolean for the last page, and the next page number.
     */
    public static <T> PaginatedResponse<T> paginate(List<T> list, int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Page number and page size must be greater than 0.");
        }

        int fromIndex = (pageNo - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, list.size());

        if (fromIndex >= list.size()) {
            return new PaginatedResponse<>(new ArrayList<>(), true, null); // Return empty list if the page is out of range
        }

        List<T> paginatedList = list.subList(fromIndex, toIndex);
        boolean isLastPage = toIndex >= list.size();
        Integer nextPageNo = isLastPage ? null : pageNo + 1;

        return new PaginatedResponse<>(paginatedList, isLastPage, nextPageNo);
    }
}
