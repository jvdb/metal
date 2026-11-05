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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Collections.unmodifiableList;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.data.Slice.createFromSource;
import static io.parsingdata.metal.expression.value.NotAValue.NOT_A_VALUE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.parsingdata.metal.ImmutableObject;
import io.parsingdata.metal.Util;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;

/**
 * A {@link ValueExpression} that splits the results of evaluating its operand
 * into individual bytes.
 * <p>
 * A Bytes expression has a single <code>operand</code> (a
 * {@link ValueExpression}). When evaluated, it evaluates <code>operand</code>
 * and instead of returning the list of results, each result is split into
 * {@link Value} objects representing each individual byte of the original
 * result.
 * <p>
 * For example, if <code>operand</code> evaluates to a list of two values, of
 * 2 and 3 bytes respectively, the Bytes expression turns this into a list of
 * 5 values, representing the individual bytes of the original results.
 */
public class Bytes extends ImmutableObject implements ValueExpression {

    public final ValueExpression operand;

    public Bytes(final ValueExpression operand) {
        this.operand = checkNotNull(operand, "operand");
    }

    /**
     * Returns a value for every byte of every value of the operand. Just like the order of the values, the order of
     * the bytes is reversed: the last byte of the last value comes first.
     */
    @Override
    public List<Value> eval(final ParseState parseState, final Encoding encoding) {
        final List<Value> values = operand.eval(parseState, encoding);
        final List<Value> result = new ArrayList<>();
        for (int i = values.size() - 1; i >= 0; i--) {
            final Value value = values.get(i);
            if (value.equals(NOT_A_VALUE)) {
                continue;
            }
            for (BigInteger b = value.length().subtract(ONE); b.compareTo(ZERO) >= 0; b = b.subtract(ONE)) {
                result.add(new CoreValue(createFromSource(value.slice().source, value.slice().offset.add(b), ONE).get(), encoding));
            }
        }
        return unmodifiableList(result);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + operand + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(operand, ((Bytes)obj).operand);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(getClass(), operand);
    }

}
