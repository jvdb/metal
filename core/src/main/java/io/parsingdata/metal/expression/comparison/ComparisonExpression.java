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

package io.parsingdata.metal.expression.comparison;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.expression.value.NotAValue.NOT_A_VALUE;

import java.util.List;
import java.util.Objects;

import io.parsingdata.metal.ImmutableObject;
import io.parsingdata.metal.Util;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;
import io.parsingdata.metal.expression.Expression;
import io.parsingdata.metal.expression.value.Value;
import io.parsingdata.metal.expression.value.ValueExpression;

/**
 * Base class for all ComparisonExpression implementations.
 * <p>
 * Specifies two input {@link ValueExpression}s that are to be evaluated and
 * their results compared. Handles evaluation of inputs and resulting lists.
 * The abstract {@link #compare(Value, Value)} method is then called for
 * individual values.
 * <p>
 * The {@link #value} argument may be <code>null</code>, in which case it is
 * not evaluated and the output value is substituted with a list containing
 * only the {@link Value} most recently added to the {@link ParseState}.
 */
public abstract class ComparisonExpression extends ImmutableObject implements Expression {

    public final ValueExpression value;
    public final ValueExpression predicate;

    protected ComparisonExpression(final ValueExpression value, final ValueExpression predicate) {
        this.value = value;
        this.predicate = checkNotNull(predicate, "predicate");
    }

    @Override
    public boolean eval(final ParseState parseState, final Encoding encoding) {
        final List<Value> values = value == null
            ? parseState.order.current()
                .map(List::<Value>of)
                .orElseGet(List::of)
            : value.eval(parseState, encoding);
        if (values.isEmpty()) {
            return false;
        }
        final List<Value> predicates = predicate.eval(parseState, encoding);
        if (values.size() != predicates.size()) {
            return false;
        }
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(NOT_A_VALUE) || predicates.get(i).equals(NOT_A_VALUE) || !compare(values.get(i), predicates.get(i))) {
                return false;
            }
        }
        return true;
    }

    public abstract boolean compare(final Value left, final Value right);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + (value == null ? "" : value + ",") + predicate + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(value, ((ComparisonExpression)obj).value)
            && Objects.equals(predicate, ((ComparisonExpression)obj).predicate);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(getClass(), value, predicate);
    }

}
