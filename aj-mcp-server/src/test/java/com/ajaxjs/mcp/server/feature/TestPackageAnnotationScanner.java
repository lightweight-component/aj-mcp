package com.ajaxjs.mcp.server.feature;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

class TestPackageAnnotationScanner {
    @Test
    void testScan() throws IOException, ClassNotFoundException {
        Set<Class<?>> classesWithAnnotation = PackageAnnotationScanner.findClassesWithAnnotation("com.ajaxjs.mcp.server.testcase", McpService.class);
        for (Class<?> clazz : classesWithAnnotation) {
            System.out.println(clazz.getName());
        }
    }
}
