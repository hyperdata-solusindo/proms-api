package com.hyperdata.proms.common.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Enterprise In-Memory Lookup & Batch Aggregation Utility.
 * <p>
 * Core engine for <b>The Golden Stack Pattern</b> (Caffeine Cache + Lookups + MapStruct):
 * <ul>
 *     <li><b>Static 1-Liners:</b> {@code Lookups.extractIds(...)}, {@code Lookups.toMap(...)}, {@code Lookups.groupingBy(...)}</li>
 *     <li><b>Fluent Chaining:</b> {@code Lookups.of(items).distinctBy(...).groupingBy(...)}</li>
 * </ul>
 * <p>
 * Guarantees insertion order preservation via {@link LinkedHashMap} and {@link LinkedHashSet},
 * handles null collections and empty states gracefully, prevents duplicate key collisions,
 * and eliminates N+1 latency across modular monolith boundaries.
 */
public final class Lookups {

    private static final Map<Class<?>, Optional<Method>> STATUS_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Method>> DELETED_METHODS_CACHE = new ConcurrentHashMap<>();

    private Lookups() {
        // Prevent instantiation of static utility class
    }

    // =========================================================================
    // Fluent Entry Point
    // =========================================================================

    /**
     * Wrap a collection in a fluent {@link LookupChain} for chaining operations.
     *
     * @param items source collection (null-safe)
     * @param <T>   element type
     * @return fluent chain instance
     */
    public static <T> LookupChain<T> of(Collection<T> items) {
        return new LookupChain<>(items);
    }

    // =========================================================================
    // ID & Key Extraction (Anti-N+1 Batch Input)
    // =========================================================================

    /**
     * Extracts a {@link Set} of unique IDs/keys, discarding nulls and blank strings.
     * Preserves discovery order using {@link LinkedHashSet}.
     *
     * @param items       source collection
     * @param idExtractor function extracting the ID
     * @param <T>         source element type
     * @param <ID>        extracted ID type
     * @return non-null LinkedHashSet of unique IDs
     */
    public static <T, ID> Set<ID> extractIds(Collection<T> items, Function<T, ID> idExtractor) {
        if (items == null || items.isEmpty() || idExtractor == null) {
            return Collections.emptySet();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(idExtractor)
                .filter(Objects::nonNull)
                .filter(id -> !(id instanceof String s) || !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Extracts and aggregates a {@link Set} of unique IDs/keys from multiple extractors across a single collection,
     * discarding nulls and blank strings. Preserves discovery order using {@link LinkedHashSet}.
     *
     * @param items        source collection
     * @param idExtractors varargs functions extracting IDs
     * @param <T>          source element type
     * @param <ID>         extracted ID type
     * @return non-null LinkedHashSet of aggregated unique IDs
     */
    @SafeVarargs
    public static <T, ID> Set<ID> extractAllIds(Collection<T> items, Function<T, ID>... idExtractors) {
        if (items == null || items.isEmpty() || idExtractors == null || idExtractors.length == 0) {
            return Collections.emptySet();
        }
        Set<ID> result = new LinkedHashSet<>();
        for (T item : items) {
            if (item != null) {
                for (Function<T, ID> extractor : idExtractors) {
                    if (extractor != null) {
                        ID id = extractor.apply(item);
                        if (id != null && (!(id instanceof String s) || !s.isBlank())) {
                            result.add(id);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Combines multiple collections of IDs into a single deduplicated {@link Set},
     * discarding nulls and blank strings. Preserves discovery order using {@link LinkedHashSet}.
     *
     * @param idCollections varargs or array of ID collections
     * @param <ID>          extracted ID type
     * @return non-null LinkedHashSet of combined unique IDs
     */
    @SafeVarargs
    public static <ID> Set<ID> combineIds(Collection<ID>... idCollections) {
        if (idCollections == null || idCollections.length == 0) {
            return Collections.emptySet();
        }
        Set<ID> result = new LinkedHashSet<>();
        for (Collection<ID> col : idCollections) {
            if (col != null && !col.isEmpty()) {
                for (ID id : col) {
                    if (id != null && (!(id instanceof String s) || !s.isBlank())) {
                        result.add(id);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Extracts an ordered {@link List} of values, discarding nulls and blank strings.
     * Preserves exact source order from the collection / database pagination.
     *
     * @param items     source collection
     * @param extractor function extracting the value
     * @param <T>       source element type
     * @param <R>       extracted value type
     * @return non-null List of extracted values
     */
    public static <T, R> List<R> extractList(Collection<T> items, Function<T, R> extractor) {
        if (items == null || items.isEmpty() || extractor == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(extractor)
                .filter(Objects::nonNull)
                .filter(val -> !(val instanceof String s) || !s.isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Filters unique items by a specific key/property, preserving first occurrence order.
     *
     * @param items        source collection
     * @param keyExtractor function extracting the key to determine uniqueness
     * @param <T>          element type
     * @param <K>          key type
     * @return non-null List of distinct items
     */
    public static <T, K> List<T> distinctBy(Collection<T> items, Function<T, K> keyExtractor) {
        if (items == null || items.isEmpty() || keyExtractor == null) {
            return Collections.emptyList();
        }
        Set<K> seen = new HashSet<>();
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> {
                    K key = keyExtractor.apply(item);
                    return key != null && seen.add(key);
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // =========================================================================
    // One-to-One / Many-to-One Indexing (O(1) Map Lookup)
    // =========================================================================

    /**
     * Indexes elements into a {@link Map} keyed by an extracted property.
     * Preserves order using {@link LinkedHashMap}.
     * Duplicate keys will safely keep the first element encountered without throwing exceptions.
     *
     * @param items        source collection
     * @param keyExtractor function extracting the map key
     * @param <K>          key type
     * @param <V>          value/element type
     * @return non-null LinkedHashMap
     */
    public static <K, V> Map<K, V> toMap(Collection<V> items, Function<V, K> keyExtractor) {
        return toMap(items, keyExtractor, Function.identity());
    }

    /**
     * Indexes elements into a {@link Map} with custom key and value extractors.
     * Preserves order using {@link LinkedHashMap} and avoids duplicate key collisions.
     *
     * @param items          source collection
     * @param keyExtractor   function extracting the map key
     * @param valueExtractor function extracting the map value
     * @param <T>            source element type
     * @param <K>            key type
     * @param <V>            value type
     * @return non-null LinkedHashMap
     */
    public static <T, K, V> Map<K, V> toMap(
            Collection<T> items, Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
        if (items == null || items.isEmpty() || keyExtractor == null || valueExtractor == null) {
            return Collections.emptyMap();
        }
        Map<K, V> map = new LinkedHashMap<>();
        for (T item : items) {
            if (item != null) {
                K key = keyExtractor.apply(item);
                if (key != null) {
                    map.putIfAbsent(key, valueExtractor.apply(item));
                }
            }
        }
        return map;
    }

    // =========================================================================
    // One-to-Many Grouping
    // =========================================================================

    /**
     * Groups elements by key into a {@code Map<K, List<V>>}.
     * Preserves key insertion order using {@link LinkedHashMap}.
     *
     * @param items        source collection
     * @param keyExtractor function extracting grouping key
     * @param <K>          grouping key type
     * @param <V>          element type
     * @return non-null LinkedHashMap of grouped lists
     */
    public static <K, V> Map<K, List<V>> groupingBy(Collection<V> items, Function<V, K> keyExtractor) {
        return groupingBy(items, keyExtractor, Function.identity());
    }

    /**
     * Groups elements by key and transforms each child element into a custom value type.
     *
     * @param items          source collection
     * @param keyExtractor   function extracting grouping key
     * @param valueExtractor function mapping element to desired grouped value
     * @param <T>            source element type
     * @param <K>            grouping key type
     * @param <V>            transformed value type
     * @return non-null LinkedHashMap of grouped lists
     */
    public static <T, K, V> Map<K, List<V>> groupingBy(
            Collection<T> items, Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
        if (items == null || items.isEmpty() || keyExtractor == null || valueExtractor == null) {
            return Collections.emptyMap();
        }
        Map<K, List<V>> map = new LinkedHashMap<>();
        for (T item : items) {
            if (item != null) {
                K key = keyExtractor.apply(item);
                if (key != null) {
                    map.computeIfAbsent(key, k -> new ArrayList<>()).add(valueExtractor.apply(item));
                }
            }
        }
        return map;
    }

    /**
     * Counts elements matching a given predicate in a null-safe manner.
     *
     * @param items     source collection
     * @param predicate filter condition to count
     * @param <T>       source element type
     * @return count of matching non-null items
     */
    public static <T> int count(Collection<T> items, Predicate<T> predicate) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        if (predicate == null) {
            return (int) items.stream().filter(Objects::nonNull).count();
        }
        return (int) items.stream().filter(Objects::nonNull).filter(predicate).count();
    }

    /**
     * Counts non-null elements in a collection in a null-safe manner.
     *
     * @param items source collection
     * @param <T>   source element type
     * @return count of non-null items
     */
    public static <T> int count(Collection<T> items) {
        return count(items, null);
    }

    /**
     * Counts occurrences of items grouped by key.
     *
     * @param items        source collection
     * @param keyExtractor function extracting grouping key
     * @param <T>          source element type
     * @param <K>          grouping key type
     * @return non-null LinkedHashMap of counts
     */
    public static <T, K> Map<K, Long> countingBy(Collection<T> items, Function<T, K> keyExtractor) {
        if (items == null || items.isEmpty() || keyExtractor == null) {
            return Collections.emptyMap();
        }
        Map<K, Long> map = new LinkedHashMap<>();
        for (T item : items) {
            if (item != null) {
                K key = keyExtractor.apply(item);
                if (key != null) {
                    map.merge(key, 1L, Long::sum);
                }
            }
        }
        return map;
    }

    /**
     * Sums {@link BigDecimal} values grouped by key.
     *
     * @param items          source collection
     * @param keyExtractor   function extracting grouping key
     * @param valueExtractor function extracting BigDecimal amount to sum
     * @param <T>            source element type
     * @param <K>            grouping key type
     * @return non-null LinkedHashMap of summed amounts
     */
    public static <T, K> Map<K, BigDecimal> summingBy(
            Collection<T> items, Function<T, K> keyExtractor, Function<T, BigDecimal> valueExtractor) {
        if (items == null || items.isEmpty() || keyExtractor == null || valueExtractor == null) {
            return Collections.emptyMap();
        }
        Map<K, BigDecimal> map = new LinkedHashMap<>();
        for (T item : items) {
            if (item != null) {
                K key = keyExtractor.apply(item);
                if (key != null) {
                    BigDecimal val = valueExtractor.apply(item);
                    if (val != null) {
                        map.merge(key, val, BigDecimal::add);
                    }
                }
            }
        }
        return map;
    }

    /**
     * Partitions a collection into two lists: {@code true} (matching) and {@code false} (non-matching).
     *
     * @param items     source collection
     * @param predicate partitioning condition
     * @param <T>       element type
     * @return non-null Map with keys Boolean.TRUE and Boolean.FALSE
     */
    public static <T> Map<Boolean, List<T>> partitionBy(Collection<T> items, Predicate<T> predicate) {
        Map<Boolean, List<T>> result = new LinkedHashMap<>();
        result.put(Boolean.TRUE, new ArrayList<>());
        result.put(Boolean.FALSE, new ArrayList<>());

        if (items == null || items.isEmpty() || predicate == null) {
            return result;
        }

        for (T item : items) {
            if (item != null) {
                if (predicate.test(item)) {
                    result.get(Boolean.TRUE).add(item);
                } else {
                    result.get(Boolean.FALSE).add(item);
                }
            }
        }
        return result;
    }

    /**
     * Joins extracted string values with a delimiter.
     *
     * @param items           source collection
     * @param stringExtractor function extracting string
     * @param delimiter       separator (e.g. ", ")
     * @param <T>             element type
     * @return concatenated string or empty string if empty
     */
    public static <T> String joining(Collection<T> items, Function<T, String> stringExtractor, CharSequence delimiter) {
        if (items == null || items.isEmpty() || stringExtractor == null) {
            return "";
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(stringExtractor)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(delimiter != null ? delimiter : ""));
    }

    // =========================================================================
    // Domain Filtering (Soft Delete & Status)
    // =========================================================================

    /**
     * Filters out soft-deleted entities based on conventional methods (isDeleted, getDeletedAt).
     * If no deletion flag is found, the item is retained.
     *
     * @param items source collection
     * @param <T>   element type
     * @return filtered List of non-deleted items
     */
    public static <T> List<T> notDeleted(Collection<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> {
                    List<Method> methods = resolveDeletedMethods(item.getClass());
                    if (methods.isEmpty()) {
                        return true;
                    }
                    for (Method method : methods) {
                        try {
                            Object val = method.invoke(item);
                            if (val instanceof Boolean b && b) {
                                return false;
                            }
                            if (!(val instanceof Boolean) && val != null) {
                                return false;
                            }
                        } catch (Exception ignored) {
                            // ignore invocation exception
                        }
                    }
                    return true;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Filters items with custom non-deleted predicate.
     *
     * @param items                source collection
     * @param notDeletedPredicate  predicate returning true for non-deleted items
     * @param <T>                  element type
     * @return filtered List of non-deleted items
     */
    public static <T> List<T> notDeleted(Collection<T> items, Predicate<T> notDeletedPredicate) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        if (notDeletedPredicate == null) {
            return notDeleted(items);
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(notDeletedPredicate)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Filters items whose status equals "active" (case-insensitive) using a custom status extractor.
     *
     * @param items           source collection
     * @param statusExtractor function extracting status string
     * @param <T>             element type
     * @return filtered List of active items
     */
    public static <T> List<T> activeOnly(Collection<T> items, Function<T, String> statusExtractor) {
        if (items == null || items.isEmpty() || statusExtractor == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> {
                    String status = statusExtractor.apply(item);
                    return status != null && "active".equalsIgnoreCase(status.trim());
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Filters items whose status equals "active" or boolean active flag is true.
     * Automatically attempts to resolve {@code getStatus()}, {@code status()}, {@code isActive()}, or {@code getActive()}.
     *
     * @param items source collection
     * @param <T>   element type
     * @return filtered List of active items
     */
    public static <T> List<T> activeOnly(Collection<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> {
                    Method method = resolveStatusMethod(item.getClass());
                    if (method == null) {
                        return true;
                    }
                    try {
                        Object val = method.invoke(item);
                        if (val instanceof Boolean b) {
                            return b;
                        }
                        return val != null
                                && "active".equalsIgnoreCase(val.toString().trim());
                    } catch (Exception e) {
                        return true;
                    }
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // =========================================================================
    // Lazy Loading & Foreign Mapping (Zero-N+1 Auto Loader)
    // =========================================================================

    /**
     * Lazy loads and indexes foreign entities into a Map, executing the loader ONLY when extracted IDs are non-empty.
     *
     * @param sourceItems  source collection containing foreign IDs
     * @param idExtractor  function extracting foreign ID from source items
     * @param loader       repository / facade batch loader function
     * @param keyExtractor function extracting Map key from loaded entity
     * @param <T>          source element type
     * @param <ID>         ID type
     * @param <R>          loaded entity type
     * @param <K>          Map key type
     * @return non-null LinkedHashMap indexed by key
     */
    public static <T, ID, R, K> Map<K, R> loadMap(
            Collection<T> sourceItems,
            Function<T, ID> idExtractor,
            Function<Collection<ID>, Collection<R>> loader,
            Function<R, K> keyExtractor) {
        Set<ID> ids = extractIds(sourceItems, idExtractor);
        if (ids.isEmpty() || loader == null || keyExtractor == null) {
            return Collections.emptyMap();
        }
        return toMap(loader.apply(ids), keyExtractor);
    }

    /**
     * Lazy loads and indexes foreign entities directly from a collection of IDs.
     *
     * @param ids          collection of IDs
     * @param loader       repository / facade batch loader function
     * @param keyExtractor function extracting Map key from loaded entity
     * @param <ID>         ID type
     * @param <R>          loaded entity type
     * @param <K>          Map key type
     * @return non-null LinkedHashMap indexed by key
     */
    public static <ID, R, K> Map<K, R> loadMap(
            Collection<ID> ids, Function<Collection<ID>, Collection<R>> loader, Function<R, K> keyExtractor) {
        if (ids == null || ids.isEmpty() || loader == null || keyExtractor == null) {
            return Collections.emptyMap();
        }
        return toMap(loader.apply(ids), keyExtractor);
    }

    /**
     * Lazy loads and indexes foreign entities into a Map with custom value transformation.
     *
     * @param sourceItems    source collection containing foreign IDs
     * @param idExtractor    function extracting foreign ID from source items
     * @param loader         repository / facade batch loader function
     * @param keyExtractor   function extracting Map key from loaded entity
     * @param valueExtractor function transforming loaded entity into desired Map value
     * @param <T>            source element type
     * @param <ID>           ID type
     * @param <R>            loaded entity type
     * @param <K>            Map key type
     * @param <V>            Map value type
     * @return non-null LinkedHashMap indexed by key
     */
    public static <T, ID, R, K, V> Map<K, V> loadMap(
            Collection<T> sourceItems,
            Function<T, ID> idExtractor,
            Function<Collection<ID>, Collection<R>> loader,
            Function<R, K> keyExtractor,
            Function<R, V> valueExtractor) {
        Set<ID> ids = extractIds(sourceItems, idExtractor);
        if (ids.isEmpty() || loader == null || keyExtractor == null || valueExtractor == null) {
            return Collections.emptyMap();
        }
        return toMap(loader.apply(ids), keyExtractor, valueExtractor);
    }

    // =========================================================================
    // Safe DTO Resolution Helper
    // =========================================================================

    /**
     * Safely finds a value from a lookup Map returning null if not found.
     *
     * @param map lookup map
     * @param key lookup key
     * @param <K> map key type
     * @param <V> map value type
     * @return found value or null
     */
    public static <K, V> V find(Map<K, V> map, K key) {
        return findOrDefault(map, key, null);
    }

    /**
     * Safely finds a value from a lookup Map with a fallback default.
     *
     * @param map          lookup map
     * @param key          lookup key
     * @param defaultValue fallback default value
     * @param <K>          map key type
     * @param <V>          map value type
     * @return found value or defaultValue
     */
    public static <K, V> V find(Map<K, V> map, K key, V defaultValue) {
        return findOrDefault(map, key, defaultValue);
    }

    /**
     * Safely finds a property value from a lookup Map without cascading null checks.
     *
     * @param map               lookup map
     * @param key               lookup key
     * @param propertyExtractor function to extract property from found value
     * @param defaultValue      fallback default value if not found or null
     * @param <K>               map key type
     * @param <V>               map value type
     * @param <R>               property return type
     * @return extracted property value or defaultValue
     */
    public static <K, V, R> R find(Map<K, V> map, K key, Function<V, R> propertyExtractor, R defaultValue) {
        if (map == null || key == null || propertyExtractor == null) {
            return defaultValue;
        }
        V val = map.get(key);
        if (val == null) {
            return defaultValue;
        }
        R prop = propertyExtractor.apply(val);
        return prop != null ? prop : defaultValue;
    }

    /**
     * Safely gets a value from a lookup Map with a fallback default.
     *
     * @param map          lookup map
     * @param key          lookup key
     * @param defaultValue fallback default value
     * @param <K>          map key type
     * @param <V>          map value type
     * @return found value or defaultValue
     */
    public static <K, V> V findOrDefault(Map<K, V> map, K key, V defaultValue) {
        if (map == null || key == null) {
            return defaultValue;
        }
        return map.getOrDefault(key, defaultValue);
    }

    /**
     * Safely extracts a property from a lookup Map value with a fallback default.
     *
     * @param map               lookup map
     * @param key               lookup key
     * @param propertyExtractor function to extract property from found value
     * @param defaultValue      fallback default value if not found or null
     * @param <K>               map key type
     * @param <V>               map value type
     * @param <R>               property return type
     * @return extracted property value or defaultValue
     */
    public static <K, V, R> R findOrDefault(Map<K, V> map, K key, Function<V, R> propertyExtractor, R defaultValue) {
        return find(map, key, propertyExtractor, defaultValue);
    }

    /**
     * Safely resolves a single entity/contract using a loader function if the ID is valid (non-null and non-blank).
     * Unwraps {@link Optional} results automatically if present.
     *
     * @param id     lookup key / entity ID
     * @param loader loader function taking ID
     * @param <ID>   ID type
     * @param <R>    resolved entity type
     * @return resolved object or null if ID is blank/null or not found
     */
    @SuppressWarnings("unchecked")
    public static <ID, R> R resolve(ID id, Function<ID, ?> loader) {
        if (id == null || loader == null) {
            return null;
        }
        if (id instanceof String s && s.isBlank()) {
            return null;
        }
        Object result = loader.apply(id);
        if (result instanceof Optional<?> opt) {
            return (R) opt.orElse(null);
        }
        return (R) result;
    }

    /**
     * Transforms a collection into a list using a mapping function in a null-safe manner.
     *
     * @param items  source collection
     * @param mapper mapping function
     * @param <T>    source element type
     * @param <R>    target result type
     * @return non-null List of mapped elements
     */
    public static <T, R> List<R> map(Collection<T> items, Function<T, R> mapper) {
        if (items == null || items.isEmpty() || mapper == null) {
            return Collections.emptyList();
        }
        return items.stream().filter(Objects::nonNull).map(mapper).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Transforms a collection into a flattened list using a mapping function in a null-safe manner.
     *
     * @param items  source collection
     * @param mapper mapping function returning a collection or streamable elements
     * @param <T>    source element type
     * @param <R>    target result type
     * @return non-null List of flattened elements
     */
    public static <T, R> List<R> flatMap(Collection<T> items, Function<T, ? extends Collection<R>> mapper) {
        if (items == null || items.isEmpty() || mapper == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Transforms a collection by looking up related values from a Map and mapping with a {@link BiFunction}.
     *
     * @param items        source collection
     * @param keyExtractor function extracting the lookup key from source item
     * @param lookupMap    map containing related lookup data
     * @param mapper       bi-function taking (sourceItem, lookupValue) and returning target result
     * @param <T>          source element type
     * @param <K>          lookup key type
     * @param <L>          lookup value type
     * @param <R>          target result type
     * @return non-null List of mapped elements
     */
    public static <T, K, L, R> List<R> map(
            Collection<T> items, Function<T, K> keyExtractor, Map<K, L> lookupMap, BiFunction<T, L, R> mapper) {
        if (items == null || items.isEmpty() || mapper == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    K key = keyExtractor != null ? keyExtractor.apply(item) : null;
                    L lookupVal = (lookupMap != null && key != null) ? lookupMap.get(key) : null;
                    return mapper.apply(item, lookupVal);
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // =========================================================================
    // Internal Helpers
    // =========================================================================

    private static Method resolveStatusMethod(Class<?> clazz) {
        return STATUS_METHOD_CACHE
                .computeIfAbsent(clazz, c -> {
                    for (String name : List.of("isActive", "getActive", "getStatus", "status")) {
                        try {
                            return Optional.of(c.getMethod(name));
                        } catch (NoSuchMethodException ignored) {
                            // Try next candidate
                        }
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }

    private static List<Method> resolveDeletedMethods(Class<?> clazz) {
        return DELETED_METHODS_CACHE.computeIfAbsent(clazz, c -> {
            List<Method> methods = new ArrayList<>();
            for (String name : List.of("isDeleted", "getDeleted", "getDeletedAt", "deletedAt")) {
                try {
                    methods.add(c.getMethod(name));
                } catch (NoSuchMethodException ignored) {
                    // Try next candidate
                }
            }
            return methods;
        });
    }

    // =========================================================================
    // Fluent Pipeline Implementation (LookupChain & LookupIdChain)
    // =========================================================================

    /**
     * Fluent chain wrapper for collections, enabling chained transformations, grouping, and ID extraction.
     *
     * @param <T> element type
     */
    public static class LookupChain<T> {

        private final List<T> elements;

        public LookupChain(Collection<T> items) {
            if (items == null || items.isEmpty()) {
                this.elements = Collections.emptyList();
            } else {
                this.elements =
                        items.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
            }
        }

        public LookupChain<T> filter(Predicate<T> predicate) {
            if (elements.isEmpty() || predicate == null) {
                return this;
            }
            List<T> filtered = elements.stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
            return new LookupChain<>(filtered);
        }

        public LookupChain<T> nonNull() {
            return this;
        }

        public LookupChain<T> notDeleted() {
            return new LookupChain<>(Lookups.notDeleted(elements));
        }

        public LookupChain<T> notDeleted(Predicate<T> notDeletedPredicate) {
            return new LookupChain<>(Lookups.notDeleted(elements, notDeletedPredicate));
        }

        public LookupChain<T> activeOnly() {
            return new LookupChain<>(Lookups.activeOnly(elements));
        }

        public LookupChain<T> activeOnly(Function<T, String> statusExtractor) {
            return new LookupChain<>(Lookups.activeOnly(elements, statusExtractor));
        }

        public <K> LookupChain<T> distinctBy(Function<T, K> keyExtractor) {
            return new LookupChain<>(Lookups.distinctBy(elements, keyExtractor));
        }

        public <R> LookupChain<R> extract(Function<T, R> extractor) {
            return new LookupChain<>(Lookups.extractList(elements, extractor));
        }

        /**
         * Transitions to a {@link LookupIdChain} for fluent lazy loading of foreign relations.
         *
         * @param idExtractor function extracting the ID
         * @param <ID>        ID type
         * @return fluent ID chain
         */
        public <ID> LookupIdChain<ID> extractIds(Function<T, ID> idExtractor) {
            return new LookupIdChain<>(Lookups.extractIds(elements, idExtractor));
        }

        /**
         * Transitions to a {@link LookupIdChain} aggregating multiple foreign IDs from each element.
         *
         * @param idExtractors varargs functions extracting IDs
         * @param <ID>         ID type
         * @return fluent ID chain
         */
        @SafeVarargs
        public final <ID> LookupIdChain<ID> extractAllIds(Function<T, ID>... idExtractors) {
            return new LookupIdChain<>(Lookups.extractAllIds(elements, idExtractors));
        }

        public <R> List<R> extractList(Function<T, R> extractor) {
            return Lookups.extractList(elements, extractor);
        }

        public <K> Map<K, T> toMap(Function<T, K> keyExtractor) {
            return Lookups.toMap(elements, keyExtractor);
        }

        public <K, V> Map<K, V> toMap(Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
            return Lookups.toMap(elements, keyExtractor, valueExtractor);
        }

        public <K> Map<K, List<T>> groupingBy(Function<T, K> keyExtractor) {
            return Lookups.groupingBy(elements, keyExtractor);
        }

        public <K, V> Map<K, List<V>> groupingBy(Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
            return Lookups.groupingBy(elements, keyExtractor, valueExtractor);
        }

        public <K> Map<K, Long> countingBy(Function<T, K> keyExtractor) {
            return Lookups.countingBy(elements, keyExtractor);
        }

        public <K> Map<K, BigDecimal> summingBy(Function<T, K> keyExtractor, Function<T, BigDecimal> valueExtractor) {
            return Lookups.summingBy(elements, keyExtractor, valueExtractor);
        }

        public Map<Boolean, List<T>> partitionBy(Predicate<T> predicate) {
            return Lookups.partitionBy(elements, predicate);
        }

        public String joining(Function<T, String> stringExtractor, CharSequence delimiter) {
            return Lookups.joining(elements, stringExtractor, delimiter);
        }

        public <R> List<R> map(Function<T, R> mapper) {
            return Lookups.map(elements, mapper);
        }

        public <K, L, R> List<R> map(Function<T, K> keyExtractor, Map<K, L> lookupMap, BiFunction<T, L, R> mapper) {
            return Lookups.map(elements, keyExtractor, lookupMap, mapper);
        }

        public int count() {
            return elements.size();
        }

        public int count(Predicate<T> predicate) {
            return Lookups.count(elements, predicate);
        }

        public List<T> toList() {
            return new ArrayList<>(elements);
        }

        public Set<T> toSet() {
            return new LinkedHashSet<>(elements);
        }
    }

    /**
     * Fluent ID chain for batch foreign lookup and loading.
     *
     * @param <ID> ID type
     */
    public static class LookupIdChain<ID> {

        private final Set<ID> ids;

        public LookupIdChain(Set<ID> ids) {
            this.ids = ids != null ? ids : Collections.emptySet();
        }

        /**
         * Loads foreign entities using a batch loader only when IDs are not empty.
         *
         * @param loader repository or facade loader function
         * @param <R>    loaded entity type
         * @return LookupChain of loaded entities
         */
        public <R> LookupChain<R> load(Function<Collection<ID>, Collection<R>> loader) {
            if (ids.isEmpty() || loader == null) {
                return new LookupChain<>(Collections.emptyList());
            }
            return new LookupChain<>(loader.apply(ids));
        }

        /**
         * Loads foreign entities and indexes them into a Map in a single fluent operation.
         *
         * @param loader       repository or facade loader function
         * @param keyExtractor function extracting Map key
         * @param <R>          loaded entity type
         * @param <K>          Map key type
         * @return non-null LinkedHashMap
         */
        public <R, K> Map<K, R> loadMap(Function<Collection<ID>, Collection<R>> loader, Function<R, K> keyExtractor) {
            return load(loader).toMap(keyExtractor);
        }

        /**
         * Loads foreign entities and indexes them into a Map with custom value extractor.
         *
         * @param loader         repository or facade loader function
         * @param keyExtractor   function extracting Map key
         * @param valueExtractor function extracting Map value
         * @param <R>            loaded entity type
         * @param <K>            Map key type
         * @param <V>            Map value type
         * @return non-null LinkedHashMap
         */
        public <R, K, V> Map<K, V> loadMap(
                Function<Collection<ID>, Collection<R>> loader,
                Function<R, K> keyExtractor,
                Function<R, V> valueExtractor) {
            return load(loader).toMap(keyExtractor, valueExtractor);
        }

        public Set<ID> toSet() {
            return new LinkedHashSet<>(ids);
        }

        public List<ID> toList() {
            return new ArrayList<>(ids);
        }

        public boolean isEmpty() {
            return ids.isEmpty();
        }

        public int size() {
            return ids.size();
        }
    }
}
