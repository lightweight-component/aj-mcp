package com.ajaxjs.mcp.server.feature;

import com.ajaxjs.mcp.protocol.McpConstant;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageAnnotationScanner {
    /**
     * 在指定的包中查找带有特定注解的类
     * 此方法首先将包名转换为路径，然后通过类加载器获取该路径下的所有资源（包括文件和JAR）
     * 对于文件资源，方法会遍历文件系统以查找类文件；对于JAR资源，方法会打开JAR文件以查找类文件
     *
     * @param packageName 需要扫描的包名，例如"com.example"
     * @param annotation  要查找的注解类型，例如MyAnnotation.class
     * @return 一个包含所有找到的带有指定注解的类的集合
     * @throws IOException            如果无法读取资源
     * @throws ClassNotFoundException 如果类加载失败
     */
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

    /**
     * 在指定目录下递归查找带有特定注解的类
     *
     * @param directory   目录文件对象，代表要扫描的目录
     * @param packageName 扫描的包名，用于构建类的全限定名
     * @param annotation  要查找的注解类，仅添加带有此注解的类
     * @param classes     用于存储找到的类的集合
     * @throws ClassNotFoundException 当类加载失败时抛出
     */
    private static void findClassesInDirectory(File directory, String packageName, Class<? extends Annotation> annotation, Set<Class<?>> classes) throws ClassNotFoundException {
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                // 递归子目录
                findClassesInDirectory(file, packageName + "." + file.getName(), annotation, classes);
            } else if (file.getName().endsWith(CLASS_FILE_EXTENSION)) {
                // 加载类并检查是否带有目标注解
                String className = packageName + "." + file.getName().replace(CLASS_FILE_EXTENSION, McpConstant.EMPTY_STR);
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(annotation))
                    classes.add(clazz);
            }
        }
    }

    private static final String CLASS_FILE_EXTENSION = ".class";

    private static void findClassesInJar(JarFile jarFile, String packagePath, Class<? extends Annotation> annotation, Set<Class<?>> classes) throws ClassNotFoundException {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.startsWith(packagePath) && entryName.endsWith(CLASS_FILE_EXTENSION)) {
                // 加载类并检查是否带有目标注解
                String className = entryName.replace('/', '.').replace(CLASS_FILE_EXTENSION, McpConstant.EMPTY_STR);
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(annotation))
                    classes.add(clazz);

            }
        }
    }
}