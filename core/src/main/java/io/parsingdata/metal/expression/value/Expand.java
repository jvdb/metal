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

package io.parsingdata.metal.expression.value;

import static java.util.Collections.unmodifiableList;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.expression.value.NotAValue.NOT_A_VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.parsingdata.metal.ImmutableObject;
import io.parsingdata.metal.Util;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;

/**
 * A {@link ValueExpression} that expands a result by copying and concatenating
 * it a specified amount of times.
 * <p>
 * An Expand expression has two operands: <code>bases</code> (a
 * {@link ValueExpression}) and <code>count</code> (a
 * {@link SingleValueExpression}). Both operands are evaluated. Multiple copies
 * of the result of evaluating <code>bases</code> are concatenated. The amount
 * of copies equals the result of evaluating <code>count</code>. If
 * <code>count</code> evaluated to an empty value or <code>NOT_A_VALUE</code>,
 * an {@link IllegalArgumentException} is thrown.
 */
public class Expand extends ImmutableObject implements ValueExpression {

    public final ValueExpression bases;
    public final SingleValueExpression count;

    public Expand(final ValueExpression bases, final SingleValueExpression count) {
        this.bases = checkNotNull(bases, "bases");
        this.count = checkNotNull(count, "count");
    }

    @Override
    public List<Value> eval(final ParseState parseState, final Encoding encoding) {
        final List<Value> baseList = bases.eval(parseState, encoding);
        if (baseList.isEmpty()) {
            return baseList;
        }
        return count.evalSingle(parseState, encoding)
            .filter(countValue -> !countValue.equals(NOT_A_VALUE))
            .map(countValue -> expand(baseList, countValue.asNumeric().intValueExact()))
            .orElseThrow(() -> new IllegalArgumentException("Count must evaluate to a non-empty countable value."));
    }

    private List<Value> expand(final List<Value> baseList, final int countValue) {
        final List<Value> result = new ArrayList<>();
        for (int i = 0; i < countValue; i++) {
            result.addAll(baseList);
        }
        return unmodifiableList(result);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bases + "," + count + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(bases, ((Expand)obj).bases)
            && Objects.equals(count, ((Expand)obj).count);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(getClass(), bases, count);
    }

}
