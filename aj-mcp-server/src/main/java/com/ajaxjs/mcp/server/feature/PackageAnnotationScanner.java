package com.ajaxjs.mcp.server.feature;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageAnnotationScanner {

    public static Set<Class<?>> findClassesWithAnnotation(String packageName, Class<? extends Annotation> annotation) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        String packagePath = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                // 处理本地文件系统中的 .class 文件
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    findClassesInDirectory(directory, packageName, annotation, classes);
                }
            } else if ("jar".equals(resource.getProtocol())) {
                // 处理 JAR 文件
                JarURLConnection jarConnection = (JarURLConnection) resource.openConnection();
                try (JarFile jarFile = jarConnection.getJarFile()) {
                    findClassesInJar(jarFile, packagePath, annotation, classes);
                }
            }
        }

        return classes;
    }

    private static void findClassesInDirectory(File directory, String packageName, Class<? extends Annotation> annotation, Set<Class<?>> classes) throws ClassNotFoundException {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                // 递归子目录
                findClassesInDirectory(file, packageName + "." + file.getName(), annotation, classes);
            } else if (file.getName().endsWith(".class")) {
                // 加载类并检查是否带有目标注解
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(annotation))
                    classes.add(clazz);

            }
        }
    }

    private static void findClassesInJar(JarFile jarFile, String packagePath, Class<? extends Annotation> annotation, Set<Class<?>> classes) throws ClassNotFoundException {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                // 加载类并检查是否带有目标注解
                String className = entryName.replace('/', '.').replace(".class", "");
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(annotation))
                    classes.add(clazz);

            }
        }
    }
}