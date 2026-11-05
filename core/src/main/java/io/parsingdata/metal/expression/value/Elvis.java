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
import java.util.Optional;

import io.parsingdata.metal.ImmutableObject;
import io.parsingdata.metal.Util;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;

/**
 * A {@link ValueExpression} that implements the Elvis operator:
 * <pre>?:</pre>.
 * <p>
 * An Elvis expression has two operands: <code>left</code> and
 * <code>right</code> (both {@link ValueExpression}s). Both operands are
 * evaluated. The return value is a list with the size of the longest list
 * returned by the two evaluations. At each index, the value at that index in
 * the result returned by evaluating <code>left</code> is placed, except if it
 * does not exist or is {@link Optional#empty()}, in which case the value at
 * that index in the result returned by evaluating right is placed there.
 */
public class Elvis extends ImmutableObject implements ValueExpression {

    public final ValueExpression left;
    public final ValueExpression right;

    public Elvis(final ValueExpression left, final ValueExpression right) {
        this.left = checkNotNull(left, "left");
        this.right = checkNotNull(right, "right");
    }

    @Override
    public List<Value> eval(final ParseState parseState, final Encoding encoding) {
        final List<Value> leftValues = left.eval(parseState, encoding);
        final List<Value> rightValues = right.eval(parseState, encoding);
        final int pairCount = Math.min(leftValues.size(), rightValues.size());
        final List<Value> result = new ArrayList<>(Math.max(leftValues.size(), rightValues.size()));
        for (int i = 0; i < pairCount; i++) {
            result.add(leftValues.get(i).equals(NOT_A_VALUE) ? rightValues.get(i) : leftValues.get(i));
        }
        // Add the remaining values of the longest list.
        result.addAll(leftValues.subList(pairCount, leftValues.size()));
        result.addAll(rightValues.subList(pairCount, rightValues.size()));
        return unmodifiableList(result);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + left + "," + right + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(left, ((Elvis)obj).left)
            && Objects.equals(right, ((Elvis)obj).right);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(getClass(), left, right);
    }

}
