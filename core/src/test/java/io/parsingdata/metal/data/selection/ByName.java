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

package io.parsingdata.metal.data.selection;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.Util.reverse;

import java.util.List;
import java.util.stream.Collectors;
import io.parsingdata.metal.data.ParseGraph;
import io.parsingdata.metal.data.ParseValue;
import io.parsingdata.metal.data.Selection;

public final class ByName {

    private ByName() {}

    /**
     * @param graph The graph to search
     * @param name Name of the value
     * @return The first value (bottom-up) with the provided name in the provided graph
     */
    public static ParseValue getValue(final ParseGraph graph, final String name) {
        return Selection.getAllValues(graph, (value) -> value.matches(name), 1).stream().findFirst().orElse(null);
    }

    /**
     * @param graph The graph to search
     * @param name Name of the value
     * @return All values with the provided name in this graph
     */
    public static List<ParseValue> getAllValues(final ParseGraph graph, final String name) {
        checkNotNull(graph, "graph");
        checkNotNull(name, "name");
        return reverse(Selection.getAllValues(graph, (value) -> value.matches(name)));
    }

    public static ParseValue get(final List<ParseValue> list, final String name) {
        return list.stream().filter(value -> value.matches(name)).findFirst().orElse(null);
    }

    public static List<ParseValue> getAll(final List<ParseValue> list, final String name) {
        return list.stream().filter(value -> value.matches(name)).collect(Collectors.toUnmodifiableList());
    }

}
