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

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;

import static io.parsingdata.metal.Util.checkNotNegative;
import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.Util.format;
import static io.parsingdata.metal.Util.reverse;
import static io.parsingdata.metal.expression.value.NotAValue.NOT_A_VALUE;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.parsingdata.metal.Util;
import io.parsingdata.metal.expression.value.Value;

public class ConcatenatedValueSource extends Source {

    /**
     * The values to concatenate, in the order in which their data appears in this source.
     */
    public final List<Value> values;
    public final BigInteger length;

    private ConcatenatedValueSource(final List<Value> values, final BigInteger length) {
        this.values = checkNotNull(values, "values");
        this.length = checkNotNegative(length, "length");
    }

    /**
     * Create a source from the data of the values. The values are concatenated in reverse order: the data of the
     * last value in the list comes first.
     */
    public static Optional<ConcatenatedValueSource> create(final List<Value> inputValues) {
        final List<Value> values = reverse(inputValues);
        final BigInteger length = calculateTotalSize(values);
        if (length.compareTo(ZERO) == 0) {
            return Optional.empty();
        }
        return Optional.of(new ConcatenatedValueSource(values, length));
    }

    private static BigInteger calculateTotalSize(final List<Value> values) {
        BigInteger size = ZERO;
        for (final Value value : values) {
            if (value.equals(NOT_A_VALUE)) {
                return ZERO;
            }
            size = size.add(value.slice().length);
        }
        return size;
    }

    @Override
    protected byte[] getData(final BigInteger offset, final BigInteger length) {
        if (!isAvailable(offset, length)) {
            throw new IllegalStateException(format("Data to read is not available (offset=%d;length=%d;source=%s).", offset, length, this));
        }
        final byte[] output = new byte[length.intValueExact()];
        BigInteger currentOffset = ZERO;
        BigInteger currentDest = ZERO;
        BigInteger remaining = length;
        for (int i = 0; remaining.compareTo(ZERO) > 0; i++) {
            final Value value = values.get(i);
            final BigInteger nextOffset = currentOffset.add(value.slice().length);
            if (nextOffset.compareTo(offset) > 0) {
                final BigInteger localOffset = offset.subtract(currentOffset).compareTo(ZERO) < 0 ? ZERO : offset.subtract(currentOffset);
                // The second argument in getData in Slice is a limit. It will return less if the end of slice is reached.
                final byte[] data = value.slice().getData(localOffset, remaining);
                System.arraycopy(data, 0, output, currentDest.intValueExact(), data.length);
                currentDest = currentDest.add(valueOf(data.length));
                remaining = remaining.subtract(valueOf(data.length));
            }
            currentOffset = nextOffset;
        }
        return output;
    }

    @Override
    protected boolean isAvailable(final BigInteger offset, final BigInteger length) {
        return checkNotNegative(length, "length").add(checkNotNegative(offset, "offset")).compareTo(this.length) <= 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + values + "(" + length + "))";
    }

    @Override
    public boolean equals(final Object obj) {
        return Util.notNullAndSameClass(this, obj)
            && Objects.equals(values, ((ConcatenatedValueSource)obj).values)
            && Objects.equals(length, ((ConcatenatedValueSource)obj).length);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(getClass(), values, length);
    }

}
