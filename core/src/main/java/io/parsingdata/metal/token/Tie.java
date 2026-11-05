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

package io.parsingdata.metal.token;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.Util.failure;
import static io.parsingdata.metal.Util.success;
import static io.parsingdata.metal.expression.value.NotAValue.NOT_A_VALUE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.parsingdata.metal.data.DataExpressionSource;
import io.parsingdata.metal.data.Environment;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;
import io.parsingdata.metal.expression.value.Value;
import io.parsingdata.metal.expression.value.ValueExpression;

/**
 * A {@link Token} that specifies a token to be parsed inside the result of a
 * provided {@link ValueExpression}.
 * <p>
 * A Tie consists of a <code>token</code> (a {@link Token}) and a
 * <code>dataExpression</code> (a {@link ValueExpression}). First
 * <code>dataExpression</code> is evaluated. Then each value is used as an
 * input to parse the token in. Tie succeeds if all parses of
 * <code>token</code> in all results succeed. Tie fails if
 * <code>dataExpression</code> evaluates to a list of values that is either
 * empty or contains an invalid value.
 *
 * @see ValueExpression
 */
public class Tie extends Token {

    public final Token token;
    public final ValueExpression dataExpression;

    public Tie(final String name, final Token token, final ValueExpression dataExpression, final Encoding encoding) {
        super(name, encoding);
        this.token = checkNotNull(token, "token");
        this.dataExpression = checkNotNull(dataExpression, "dataExpression");
    }

    @Override
    protected Optional<ParseState> parseImpl(final Environment environment) {
        final List<Value> values = dataExpression.eval(environment.parseState, environment.encoding);
        final ParseState returnParseState = environment.parseState;
        Environment currentEnvironment = environment.addBranch(this);
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).equals(NOT_A_VALUE)) {
                return failure();
            }
            final Optional<ParseState> result = token.parse(currentEnvironment.withParseState(currentEnvironment.parseState.withSource(new DataExpressionSource(dataExpression, index, currentEnvironment.parseState, currentEnvironment.encoding))));
            if (result.isEmpty()) {
                return failure();
            }
            currentEnvironment = currentEnvironment.withParseState(result.get());
        }
        return success(returnParseState.withOrder(currentEnvironment.parseState.closeBranch(this).order, currentEnvironment.parseState.cache));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + makeNameFragment() + token + "," + dataExpression + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj)
            && Objects.equals(token, ((Tie)obj).token)
            && Objects.equals(dataExpression, ((Tie)obj).dataExpression);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(super.immutableHashCode(), token, dataExpression);
    }

}
