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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static io.parsingdata.metal.Shorthand.rep;
import static io.parsingdata.metal.util.ParseStateFactory.stream;
import static io.parsingdata.metal.util.TokenDefinitions.any;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.parsingdata.metal.token.Token;

public class ParseStateTest {

    @Test
    public void closeParseStateWithWrongToken() {
        final Token open = rep("openName", any("a"));
        final Token close = rep("closeName", any("a"));
        final Exception e = Assertions.assertThrows(IllegalStateException.class, () -> stream().addBranch(open).closeBranch(close));
        assertEquals("Cannot close branch for iterable token closeName. Current iteration state is for token openName.", e.getMessage());
    }

    @Test
    public void referencesOfBranchesAreIndependent() {
        final ParseState base = stream(1, 2).add(reference(0));
        // The first branch can reuse the list of references of the base parse state, the second one cannot.
        final ParseState left = base.add(reference(1));
        final ParseState right = base.add(reference(2));

        assertEquals(List.of(reference(0)), base.references());
        assertEquals(List.of(reference(0), reference(1)), left.references());
        assertEquals(List.of(reference(0), reference(2)), right.references());
        assertEquals(List.of(reference(0), reference(1), reference(3)), left.add(reference(3)).references());
        assertEquals(List.of(reference(0), reference(2), reference(3)), right.add(reference(3)).references());

        assertTrue(left.containsReference(reference(1)));
        assertFalse(left.containsReference(reference(2)));
        assertFalse(base.containsReference(reference(1)));
        assertTrue(right.containsReference(reference(2)));
        assertFalse(right.containsReference(reference(1)));

        assertNotEquals(left, right);
        assertEquals(right, stream(1, 2).add(reference(0)).add(reference(2)));
        assertEquals(right.hashCode(), stream(1, 2).add(reference(0)).add(reference(2)).hashCode());
    }

    @Test
    public void iterations() {
        final Token outer = rep("outer", any("a"));
        final Token inner = rep("inner", any("a"));
        final ParseState state = stream(1, 2).addBranch(outer).iterate().addBranch(inner).iterate().iterate();
        assertEquals(List.of(Map.entry(inner, BigInteger.TWO), Map.entry(outer, BigInteger.ONE)), state.iterations);
        assertEquals(List.of(Map.entry(outer, BigInteger.ONE)), state.closeBranch(inner).iterations);
    }

    private static ParseReference reference(final int offset) {
        return new ParseReference(BigInteger.valueOf(offset), new ConstantSource(new byte[] { 1, 2 }), any("a"));
    }

}