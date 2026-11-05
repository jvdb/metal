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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import static io.parsingdata.metal.Util.appendShared;
import static io.parsingdata.metal.Util.checkNotNegative;
import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.Util.format;
import static io.parsingdata.metal.Util.prepend;
import static io.parsingdata.metal.Util.sharedPrefix;
import static io.parsingdata.metal.data.ParseValueCache.NO_CACHE;
import static io.parsingdata.metal.data.Slice.createFromSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.parsingdata.metal.ImmutableObject;
import io.parsingdata.metal.Util;
import io.parsingdata.metal.token.Token;

public class ParseState extends ImmutableObject {

    public final ParseGraph order;
    public final ParseValueCache cache;
    public final BigInteger offset;
    public final Source source;
    /**
     * The iteration counts of the iterable tokens currently being parsed, innermost first.
     */
    public final List<Map.Entry<Token, BigInteger>> iterations;
    // The references are stored in a list that is shared between parse states and only grows (see
    // Util.appendShared). This parse state only uses the first referenceCount elements.
    private final List<ParseReference> references;
    private final int referenceCount;

    /**
     * @param iterations the iteration counts of the iterable tokens currently being parsed, innermost first
     * @param references the references to tokens parsed so far, in the order they were added
     */
    public ParseState(final ParseGraph order, final ParseValueCache cache, final Source source, final BigInteger offset, final List<Map.Entry<Token, BigInteger>> iterations, final List<ParseReference> references) {
        this(order, cache, source, offset, List.copyOf(checkNotNull(iterations, "iterations")), new ArrayList<>(List.copyOf(checkNotNull(references, "references"))), references.size());
    }

    private ParseState(final ParseGraph order, final ParseValueCache cache, final Source source, final BigInteger offset, final List<Map.Entry<Token, BigInteger>> iterations, final List<ParseReference> references, final int referenceCount) {
        this.order = checkNotNull(order, "order");
        this.cache = checkNotNull(cache, "cache");
        this.source = checkNotNull(source, "source");
        this.offset = checkNotNegative(offset, "offset");
        this.iterations = iterations;
        this.references = references;
        this.referenceCount = referenceCount;
    }

    public static ParseState createFromByteStream(final ByteStream input, final BigInteger offset) {
        return new ParseState(ParseGraph.EMPTY, new ParseValueCache(), new ByteStreamSource(input), offset, List.of(), List.of());
    }

    public static ParseState createFromByteStream(final ByteStream input) {
        return createFromByteStream(input, ZERO);
    }

    /**
     * @return the references to tokens parsed so far, in the order they were added
     */
    public List<ParseReference> references() {
        return sharedPrefix(references, referenceCount);
    }

    public boolean containsReference(final ParseReference parseReference) {
        synchronized (references) {
            return references.subList(0, referenceCount).contains(parseReference);
        }
    }

    public ParseState addBranch(final Token token) {
        return new ParseState(order.addBranch(token), cache, source, offset, token.isIterable() ? prepend(Map.entry(token, ZERO), iterations) : iterations, references, referenceCount);
    }

    public ParseState closeBranch(final Token token) {
        if (token.isIterable() && !iterations.get(0).getKey().equals(token)) {
            throw new IllegalStateException(format("Cannot close branch for iterable token %s. Current iteration state is for token %s.", token.name, iterations.get(0).getKey().name));
        }
        return new ParseState(order.closeBranch(token), cache, source, offset, token.isIterable() ? List.copyOf(iterations.subList(1, iterations.size())) : iterations, references, referenceCount);
    }

    public ParseState add(final ParseReference parseReference) {
        return new ParseState(order, cache, source, offset, iterations, appendShared(references, referenceCount, parseReference), referenceCount + 1);
    }

    public ParseState add(final ParseValue parseValue) {
        return new ParseState(order.add(parseValue), cache.add(parseValue), source, offset, iterations, references, referenceCount);
    }

    public ParseState createCycle(final ParseReference parseReference) {
        return new ParseState(order.add(parseReference), cache, source, offset, iterations, references, referenceCount);
    }

    public ParseState iterate() {
        final Map.Entry<Token, BigInteger> current = iterations.get(0);
        return new ParseState(order, cache, source, offset, prepend(Map.entry(current.getKey(), current.getValue().add(ONE)), iterations.subList(1, iterations.size())), references, referenceCount);
    }

    public Optional<ParseState> seek(final BigInteger newOffset) {
        return newOffset.compareTo(ZERO) >= 0 ? Optional.of(new ParseState(order, cache, source, newOffset, iterations, references, referenceCount)) : Optional.empty();
    }

    public ParseState withOrder(final ParseGraph order) {
        return new ParseState(order, NO_CACHE, source, offset, iterations, references, referenceCount);
    }

    public ParseState withOrder(final ParseGraph order, final ParseValueCache cache) {
        return new ParseState(order, cache, source, offset, iterations, references, referenceCount);
    }

    public ParseState withSource(final Source source) {
        return new ParseState(order, cache, source, ZERO, iterations, references, referenceCount);
    }

    public Optional<Slice> slice(final BigInteger length) {
        return createFromSource(source, offset, length);
    }

    @Override
    public String toString() {
        final String iterationsString = iterations.isEmpty() ? "" : ";iterations:" + iterations;
        final String referencesString = referenceCount == 0 ? "" : ";references:" + references();
        return getClass().getSimpleName() + "(source:" + source + ";offset:" + offset + ";order:" + order + iterationsString + referencesString + ";" + cache + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(order, ((ParseState)obj).order)
            && Objects.equals(cache, ((ParseState)obj).cache)
            && Objects.equals(offset, ((ParseState)obj).offset)
            && Objects.equals(source, ((ParseState)obj).source)
            && Objects.equals(iterations, ((ParseState)obj).iterations)
            && Objects.equals(references(), ((ParseState)obj).references());
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(getClass(), order, cache, offset, source, iterations, references());
    }

}
