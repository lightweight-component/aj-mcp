package com.ajaxjs.mcp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.*;

public class McpUtils {
    /**
     * Ensures that the given string is not null and not blank.
     *
     * @param string The string to check.
     * @param name   The name of the string to be used in the exception message.
     * @return The string if it is not null and not blank.
     * @throws IllegalArgumentException if the string is null or blank.
     */
    public static String ensureNotBlank(String string, String name) {
        if (string == null || string.trim().isEmpty()) {
            throw illegalArgument("%s cannot be null or blank", name);
        }

        return string;
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

    /**
     * Ensures that the given collection is not null and not empty.
     *
     * @param collection The collection to check.
     * @param name       The name of the collection to be used in the exception message.
     * @param <T>        The type of the collection.
     * @return The collection if it is not null and not empty.
     * @throws IllegalArgumentException if the collection is null or empty.
     */
    public static <T extends Collection<?>> T ensureNotEmpty(T collection, String name) {
        if (collection == null || collection.isEmpty()) {
            throw illegalArgument("%s cannot be null or empty", name);
        }

        return collection;
    }

    /**
     * Ensures that the given array is not null and not empty.
     *
     * @param array The array to check.
     * @param name  The name of the array to be used in the exception message.
     * @param <T>   The component type of the array.
     * @return The array if it is not null and not empty.
     * @throws IllegalArgumentException if the array is null or empty.
     */
    public static <T> T[] ensureNotEmpty(T[] array, String name) {
        if (array == null || array.length == 0) {
            throw illegalArgument("%s cannot be null or empty", name);
        }

        return array;
    }

    /**
     * Ensures that the given map is not null and not empty.
     *
     * @param map  The map to check.
     * @param name The name of the map to be used in the exception message.
     * @param <K>  The type of the key.
     * @param <V>  The type of the value.
     * @return The map if it is not null and not empty.
     * @throws IllegalArgumentException if the collection is null or empty.
     */
    public static <K, V> Map<K, V> ensureNotEmpty(Map<K, V> map, String name) {
        if (map == null || map.isEmpty()) {
            throw illegalArgument("%s cannot be null or empty", name);
        }

        return map;
    }

    /**
     * Ensures that the given object is not null.
     *
     * @param object The object to check.
     * @param name   The name of the object to be used in the exception message.
     * @param <T>    The type of the object.
     * @return The object if it is not null.
     * @throws IllegalArgumentException if the object is null.
     */
    public static <T> T ensureNotNull(T object, String name) {
        return McpUtils.ensureNotNull(object, "%s cannot be null", name);
    }

    /**
     * Ensures that the given object is not null.
     *
     * @param object The object to check.
     * @param format The format of the exception message.
     * @param args   The arguments for the exception message.
     * @param <T>    The type of the object.
     * @return The object if it is not null.
     */
    public static <T> T ensureNotNull(T object, String format, Object... args) {
        if (object == null) {
            throw illegalArgument(format, args);
        }
        return object;
    }

    /**
     * Constructs an {@link IllegalArgumentException} with the given formatted result.
     *
     * <p>Equivalent to {@code new IllegalArgumentException(String.format(format, args))}.
     *
     * @param format the format string
     * @param args   the format arguments
     * @return the constructed exception.
     */
    public static IllegalArgumentException illegalArgument(String format, Object... args) {
        return new IllegalArgumentException(String.format(format, args));
    }

    /**
     * Returns an (unmodifiable) copy of the provided set.
     * Returns <code>null</code> if the provided set is <code>null</code>.
     *
     * @param set The set to copy.
     * @param <T> Generic type of the set.
     * @return The copy of the provided set.
     */
    public static <T> Set<T> copyIfNotNull(Set<T> set) {
        if (set == null) {
            return null;
        }

        return unmodifiableSet(set);
    }

    /**
     * Returns an (unmodifiable) copy of the provided list.
     * Returns <code>null</code> if the provided list is <code>null</code>.
     *
     * @param list The list to copy.
     * @param <T>  Generic type of the list.
     * @return The copy of the provided list.
     */
    public static <T> List<T> copyIfNotNull(List<T> list) {
        if (list == null) {
            return null;
        }

        return unmodifiableList(list);
    }


    /**
     * Returns an (unmodifiable) copy of the provided map.
     * Returns <code>null</code> if the provided map is <code>null</code>.
     *
     * @param map The map to copy.
     * @return The copy of the provided map.
     */
    public static <K, V> Map<K, V> copyIfNotNull(Map<K, V> map) {
        if (map == null) {
            return null;
        }

        return unmodifiableMap(map);
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
