/*
 * Copyright 2013-2024 Netherlands Forensic Institute
 * Copyright 2021-2024 Infix Technologies B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.parsingdata.metal.data;

import static java.util.Collections.unmodifiableList;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.Util.reverse;

import java.math.BigInteger;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.parsingdata.metal.token.Token;

public final class Selection {

    public static final int NO_LIMIT = -1;

    private Selection() {}

    public static Optional<ParseItem> findItemAtOffset(final List<ParseItem> items, final BigInteger offset, final Source source) {
        checkNotNull(items, "items");
        checkNotNull(source, "source");
        for (final ParseItem item : items) {
            if (item.isValue() && matchesLocation(item.asValue(), offset, source)) {
                return Optional.of(item);
            }
            if (item.isGraph()) {
                final ParseValue value = getLowestOffsetValue(item.asGraph());
                if (value != null && matchesLocation(value, offset, source)) {
                    return Optional.of(item);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesLocation(final ParseValue value, final BigInteger offset, final Source source) {
        return value.slice().offset.compareTo(offset) == 0 && value.slice().source.equals(source);
    }

    private static ParseValue getLowestOffsetValue(final ParseGraph root) {
        ParseValue lowest = null;
        final Deque<ParseGraph> graphs = new ArrayDeque<>();
        graphs.push(root);
        while (!graphs.isEmpty()) {
            final ParseGraph graph = graphs.pop();
            if (graph.isEmpty() || !graph.getDefinition().isLocal()) {
                continue;
            }
            graphs.push(graph.tail);
            if (graph.head.isGraph()) {
                graphs.push(graph.head.asGraph());
            } else if (graph.head.isValue()) {
                lowest = getLowest(lowest, graph.head.asValue());
            }
        }
        return lowest;
    }

    private static ParseValue getLowest(final ParseValue lowest, final ParseValue value) {
        return lowest == null || lowest.slice().offset.compareTo(value.slice().offset) > 0 ? value : lowest;
    }

    /**
     * Returns all values in the graph that match the predicate. The values are ordered from the least recently
     * added to the most recently added value.
     */
    public static List<ParseValue> getAllValues(final ParseGraph graph, final Predicate<ParseValue> predicate, final int limit) {
        checkNotNull(graph, "graph");
        final List<ParseValue> values = new ArrayList<>();
        final Deque<ParseGraph> graphs = new ArrayDeque<>();
        graphs.push(graph);
        while (!graphs.isEmpty() && values.size() != limit) {
            final ParseGraph current = graphs.pop();
            if (current.isEmpty()) {
                continue;
            }
            graphs.push(current.tail);
            if (current.head.isGraph()) {
                graphs.push(current.head.asGraph());
            } else if (current.head.isValue() && predicate.test(current.head.asValue())) {
                values.add(current.head.asValue());
            }
        }
        return reverse(values);
    }

    public static List<ParseValue> getAllValues(final ParseGraph graph, final Predicate<ParseValue> predicate) {
        return getAllValues(graph, predicate, NO_LIMIT);
    }

    public static List<ParseValue> getAllValues(final ParseGraph graph, final Predicate<ParseValue> predicate, final int limit, final int requestedScope, final int currentScope) {
        ParseGraph scopedGraph = graph;
        int scope = currentScope;
        while (!scopedGraph.isEmpty() && limit != 0) {
            if (requestedScope >= scope) {
                return getAllValues(scopedGraph, predicate, limit);
            }
            if (scopedGraph.head.getDefinition().isScopeDelimiter()) {
                scope--;
            }
            scopedGraph = scopedGraph.head.asGraph();
        }
        return List.of();
    }

    public static List<ParseItem> getAllRoots(final ParseGraph graph, final Token definition) {
        checkNotNull(graph, "graph");
        checkNotNull(definition, "definition");
        final List<ParseItem> roots = new ArrayList<>();
        // Each entry holds an item and its parent graph (null for the root).
        final Deque<Map.Entry<ParseItem, ParseGraph>> backlog = new ArrayDeque<>();
        backlog.push(new SimpleImmutableEntry<>(graph, null));
        while (!backlog.isEmpty()) {
            final Map.Entry<ParseItem, ParseGraph> entry = backlog.pop();
            final ParseItem item = entry.getKey();
            final ParseGraph parent = entry.getValue();
            if (item.getDefinition().equals(definition) && (parent == null || !parent.getDefinition().equals(definition))) {
                roots.add(item);
            }
            if (item.isGraph() && !item.asGraph().isEmpty()) {
                final ParseGraph itemGraph = item.asGraph();
                backlog.push(new SimpleImmutableEntry<>(itemGraph.head, itemGraph));
                backlog.push(new SimpleImmutableEntry<>(itemGraph.tail, itemGraph));
            }
        }
        Collections.reverse(roots);
        return unmodifiableList(roots);
    }

}
