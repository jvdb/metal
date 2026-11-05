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
 * Base class for {@link ValueExpression}s with two operands.
 * <p>
 * A BinaryValueExpression implements a ValueExpression that has two operands:
 * <code>left</code> and <code>right</code> (both {@link ValueExpression}s).
 * Both operands are themselves first evaluated. If at least one of the
 * operands evaluates to {@link Optional#empty()}, the result of the
 * ValueExpression itself will be empty as well.
 * <p>
 * For lists, values with the same index are evaluated in this manner. If
 * lists are of unequal length, the result is a list with evaluated values the
 * same size as the shortest list, appended with instances of NOT_A_VALUE to
 * match the size of the longest list.
 * <p>
 * To implement a BinaryValueExpression, only the
 * {@link #eval(Value, Value, ParseState, Encoding)} must be implemented,
 * handling the case of evaluating two values. This base class takes care of
 * evaluating the operands and handling list semantics.
 *
 * @see UnaryValueExpression
 */
public abstract class BinaryValueExpression extends ImmutableObject implements ValueExpression {

    public final ValueExpression left;
    public final ValueExpression right;

    protected BinaryValueExpression(final ValueExpression left, final ValueExpression right) {
        this.left = checkNotNull(left, "left");
        this.right = checkNotNull(right, "right");
    }

    public abstract Optional<Value> eval(final Value leftValue, final Value rightValue, final ParseState parseState, final Encoding encoding);

    @Override
    public List<Value> eval(final ParseState parseState, final Encoding encoding) {
        final List<Value> leftValues = left.eval(parseState, encoding);
        final List<Value> rightValues = right.eval(parseState, encoding);
        final int pairCount = Math.min(leftValues.size(), rightValues.size());
        final List<Value> result = new ArrayList<>(Math.max(leftValues.size(), rightValues.size()));
        for (int i = 0; i < pairCount; i++) {
            result.add(safeEval(leftValues.get(i), rightValues.get(i), parseState, encoding));
        }
        // Pad the result to the length of the longest list.
        for (int i = pairCount; i < Math.max(leftValues.size(), rightValues.size()); i++) {
            result.add(NOT_A_VALUE);
        }
        return unmodifiableList(result);
    }

    private Value safeEval(final Value leftValue, final Value rightValue, final ParseState parseState, final Encoding encoding) {
        if (leftValue.equals(NOT_A_VALUE) || rightValue.equals(NOT_A_VALUE)) {
            return NOT_A_VALUE;
        }
        return eval(leftValue, rightValue, parseState, encoding)
            .orElse(NOT_A_VALUE);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + left + "," + right + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(left, ((BinaryValueExpression)obj).left)
            && Objects.equals(right, ((BinaryValueExpression)obj).right);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(getClass(), left, right);
    }

}
