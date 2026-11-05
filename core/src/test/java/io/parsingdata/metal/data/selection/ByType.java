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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.parsingdata.metal.data.ParseGraph;
import io.parsingdata.metal.data.ParseItem;

public final class ByType {

    private ByType() {}

    public static List<Optional<ParseItem>> getReferences(final ParseGraph graph) {
        checkNotNull(graph, "graph");
        return getReferences(graph, graph);
    }

    private static List<Optional<ParseItem>> getReferences(final ParseGraph graph, final ParseGraph root) {
        if (graph.isEmpty()) {
            return List.of();
        }
        final ParseItem head = graph.head;
        if (head.isReference() && head.asReference().resolve(root).isEmpty()) { throw new IllegalStateException("A ParseReference must point to an existing graph."); }
        final List<Optional<ParseItem>> references = new ArrayList<>(head.isGraph() ? getReferences(head.asGraph(), root) : (head.isReference() ? List.of(head.asReference().resolve(root)) : List.of()));
        references.addAll(getReferences(graph.tail, root));
        return references;
    }

}
