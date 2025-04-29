package com.ajaxjs.mcp.server.feature;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageScanner {
    public static Set<Class<?>> findClassesWithInterface(String packageName, Class<?> targetInterface) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        String packagePath = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                // 处理本地文件系统中的 .class 文件
                File directory = new File(resource.getFile());
                if (directory.exists())
                    findClassesInDirectory(directory, packageName, targetInterface, classes);
            } else if (resource.getProtocol().equals("jar")) {
                // 处理 JAR 文件
                JarURLConnection jarConnection = (JarURLConnection) resource.openConnection();
                try (JarFile jarFile = jarConnection.getJarFile()) {
                    findClassesInJar(jarFile, packagePath, targetInterface, classes);
                }
            }
        }

        return classes;
    }

    private static void findClassesInDirectory(File directory, String packageName, Class<?> targetInterface, Set<Class<?>> classes) throws ClassNotFoundException {
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                // 递归子目录
                findClassesInDirectory(file, packageName + "." + file.getName(), targetInterface, classes);
            } else if (file.getName().endsWith(CLASS_FILE_EXTENSION)) {
                // 加载类并检查是否实现了目标接口
                String className = packageName + "." + file.getName().replace(CLASS_FILE_EXTENSION, "");
                Class<?> clazz = Class.forName(className);

                if (targetInterface.isAssignableFrom(clazz) && !clazz.isInterface())
                    classes.add(clazz);
            }
        }
    }

    private static final String CLASS_FILE_EXTENSION = ".class";

    private static void findClassesInJar(JarFile jarFile, String packagePath, Class<?> targetInterface, Set<Class<?>> classes) throws ClassNotFoundException {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.startsWith(packagePath) && entryName.endsWith(CLASS_FILE_EXTENSION)) {
                // 加载类并检查是否实现了目标接口
                String className = entryName.replace('/', '.').replace(CLASS_FILE_EXTENSION, "");
                Class<?> clazz = Class.forName(className);

                if (targetInterface.isAssignableFrom(clazz) && !clazz.isInterface())
                    classes.add(clazz);
            }
        }
    }
}