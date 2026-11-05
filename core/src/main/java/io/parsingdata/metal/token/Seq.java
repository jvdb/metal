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

import static io.parsingdata.metal.Util.checkContainsNoNulls;
import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.Util.failure;
import static io.parsingdata.metal.Util.prepend;
import static io.parsingdata.metal.Util.success;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.parsingdata.metal.data.Environment;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;

/**
 * A {@link Token} that specifies a dependency between a list of tokens.
 * <p>
 * A Seq consists of a list of <code>tokens</code>. If one of the tokens
 * doesn't succeed, the Seq fails. If all of the tokens succeed, the Seq will
 * succeed. Order is from left to right.
 */
public class Seq extends CycleToken {

    public final List<Token> tokens;

    public Seq(final String name, final Encoding encoding, final Token token1, final Token token2, final Token... additionalTokens) {
        super(name, encoding);
        this.tokens = prepend(checkNotNull(token1, "token1"), prepend(checkNotNull(token2, "token2"), Arrays.asList(checkContainsNoNulls(additionalTokens, "additionalTokens"))));
    }

    @Override
    protected Optional<ParseState> parseImpl(final Environment environment) {
        Environment currentEnvironment = environment.addBranch(this);
        for (final Token token : tokens) {
            final Optional<ParseState> result = token.parse(currentEnvironment);
            if (result.isEmpty()) {
                return failure();
            }
            currentEnvironment = currentEnvironment.withParseState(result.get());
        }
        return success(currentEnvironment.parseState.closeBranch(this));
    }

    @Override
    public boolean isScopeDelimiter() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + makeNameFragment() + tokens + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj)
            && Objects.equals(tokens, ((Seq)obj).tokens);
    }

    @Override
    public int immutableHashCode() {
        return Objects.hash(super.immutableHashCode(), tokens);
    }

}
