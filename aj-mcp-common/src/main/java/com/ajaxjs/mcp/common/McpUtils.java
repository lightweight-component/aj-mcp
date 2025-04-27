package com.ajaxjs.mcp.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class McpUtils {
    /**
     * Check whether the given {@code String} contains actual <em>text</em>.
     * <p>More specifically, this method returns {@code true} if the
     * {@code String} is not {@code null}, its length is greater than 0,
     * and it contains at least one non-whitespace character.
     *
     * @param str the {@code String} to check (maybe {@code null})
     * @return {@code true} if the {@code String} is not {@code null}, its
     * length is greater than 0, and it does not contain whitespace only
     * @see Character#isWhitespace
     */
    public static boolean hasText(String str) {
        return (str != null && !str.isEmpty() && containsText(str));
    }

    public static boolean isEmptyText(String str) {
        return !hasText(str);
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i)))
                return true;
        }

        return false;
    }

    /**
     * Creates a HashMap with a specified expected number of entries.
     * The initial capacity and load factor are calculated to minimize resizing.
     *
     * @param expectedSize the expected number of entries in the map
     * @return a new HashMap with optimal initial capacity and load factor
     */
    public static <K, V> Map<K, V> mapOf(int expectedSize) {
        // Calculate the initial capacity as the next power of two greater than or equal to expectedSize / default_load_factor
        float defaultLoadFactor = 0.75f;
        int initialCapacity = (int) Math.ceil(expectedSize / defaultLoadFactor);
        initialCapacity = Integer.highestOneBit(initialCapacity - 1) << 1;

        // Ensure that the initial capacity is at least 16 (the default capacity)
        if (initialCapacity < 16)
            initialCapacity = 16;

        return new HashMap<>(initialCapacity, defaultLoadFactor);// Create and return the HashMap with the calculated initial capacity and default load factor
    }

    /**
     * 创建一个新的 HashMap
     *
     * @param k1  键1
     * @param v1  值1
     * @param <K> k1 类型
     * @param <V> v1 类型
     * @return 新创建的 HashMap
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);

        return map;
    }

    /**
     * 创建一个新的 HashMap
     *
     * @param k1  键1
     * @param v1  值1
     * @param k2  键2
     * @param v2  值2
     * @param <K> k1 类型
     * @param <V> v1 类型
     * @return 新创建的 HashMap
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     * 创建一个新的 HashMap
     *
     * @param k1  键1
     * @param v1  值1
     * @param k2  键2
     * @param v2  值2
     * @param k3  键3
     * @param v3  值3
     * @param <K> k1 类型
     * @param <V> v1 类型
     * @return 新创建的 HashMap
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = mapOf(3);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     * Returns the given object's {@code toString()} surrounded by quotes.
     *
     * <p>If the given object is {@code null}, the string {@code "null"} is returned.
     *
     * @param object The object to quote.
     * @return The given object surrounded by quotes.
     */
    public static String quoted(Object object) {
        if (object == null) {
            return "null";
        }
        return "\"" + object + "\"";
    }

    // 模拟 failedFuture 的静态方法
    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable); // 标记为异常完成

        return future;
    }

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
