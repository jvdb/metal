package io.parsingdata.metal.data;

import static java.util.Collections.unmodifiableList;

import static io.parsingdata.metal.Util.appendShared;
import static io.parsingdata.metal.Util.sharedPrefix;
import static io.parsingdata.metal.data.Selection.NO_LIMIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.parsingdata.metal.Util;
import io.parsingdata.metal.expression.value.Value;
import io.parsingdata.metal.token.Token;
public class ParseValueCache {

    public static final ParseValueCache NO_CACHE = new ParseValueCache(null, null);

    // Per short name, a list of values in the order they were added. These lists are shared between caches and only
    // grow (see Util.appendShared), so each cache stores how many elements of each list belong to it in sizes.
    private final Map<String, List<ParseValue>> cache;
    private final Map<String, Integer> sizes;

    /**
     * Start a cache that keeps track of values added to the parse graph.
     * <p>
     * In case no caching is desired, {@link #NO_CACHE} should be used instead.
     */
    public ParseValueCache() {
        this(new HashMap<>(), new HashMap<>());
    }

    // For internal use only. It is private to avoid setting the cache to null. The NO_CACHE constant should be used instead.
    private ParseValueCache(final Map<String, List<ParseValue>> cache, final Map<String, Integer> sizes) {
        this.cache = cache;
        this.sizes = sizes;
    }

    /**
     * Find the values matching the scopeName, ordered from the most recently added to the least recently added.
     */
    public Optional<List<Value>> find(final String scopeName, final int limit) {
        if (this == NO_CACHE) {
            return Optional.empty();
        }
        final String name = shortName(scopeName);
        final List<ParseValue> values = cache.get(name);
        if (values == null) {
            return Optional.of(List.of());
        }
        final List<Value> result = new ArrayList<>();
        synchronized (values) {
            for (int i = sizes.get(name) - 1; i >= 0 && (limit == NO_LIMIT || result.size() < limit); i--) {
                final ParseValue value = values.get(i);
                if (value.matches(scopeName)) {
                    result.add(value);
                }
            }
        }
        return Optional.of(unmodifiableList(result));
    }

    public ParseValueCache add(final ParseValue value) {
        if (this == NO_CACHE) {
            return NO_CACHE;
        }
        final String name = shortName(value.name);
        final int size = sizes.getOrDefault(name, 0);
        final Map<String, List<ParseValue>> newCache = new HashMap<>(cache);
        newCache.put(name, appendShared(cache.get(name), size, value));
        final Map<String, Integer> newSizes = new HashMap<>(sizes);
        newSizes.put(name, size + 1);
        return new ParseValueCache(newCache, newSizes);
    }

    private static String shortName(final String name) {
        return name.substring(name.lastIndexOf(Token.SEPARATOR) + 1);
    }

    private Map<String, List<ParseValue>> contents() {
        if (this == NO_CACHE) {
            return null;
        }
        final Map<String, List<ParseValue>> contents = new HashMap<>();
        cache.forEach((name, values) -> contents.put(name, sharedPrefix(values, sizes.get(name))));
        return contents;
    }

    @Override
    public String toString() {
        if (this == NO_CACHE) {
            return "no-cache";
        }
        return "cache:size=" + cache.size();
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(contents(), ((ParseValueCache)obj).contents());
    }

    @Override
    public int hashCode() {
        return Objects.hash(contents());
    }
}
