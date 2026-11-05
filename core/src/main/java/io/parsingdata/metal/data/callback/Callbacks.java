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

package io.parsingdata.metal.data.callback;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.Util.prepend;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.token.Token;

public class Callbacks {

    public static final Callbacks NONE = new Callbacks(null, List.of());

    public final Callback genericCallback;
    /**
     * The callbacks for specific tokens, most recently added first.
     */
    public final List<Map.Entry<Token, Callback>> tokenCallbacks;

    private Callbacks(final Callback genericCallback, final List<Map.Entry<Token, Callback>> tokenCallbacks) {
        this.genericCallback = genericCallback;
        this.tokenCallbacks = checkNotNull(tokenCallbacks, "tokenCallbacks");
    }

    public static Callbacks create() {
        return NONE;
    }

    public Callbacks add(final Callback genericCallback) {
        return new Callbacks(genericCallback, tokenCallbacks);
    }

    public Callbacks add(final Token token, final Callback callback) {
        return new Callbacks(genericCallback, prepend(Map.entry(token, callback), tokenCallbacks));
    }

    public static Consumer<Callback> success(final Token token, final ParseState before, final ParseState after) {
        return callback -> callback.handleSuccess(token, before, after);
    }

    public static Consumer<Callback> failure(final Token token, final ParseState before) {
        return callback -> callback.handleFailure(token, before);
    }

    public void handle(final Token token, final Consumer<Callback> handler) {
        if (genericCallback != null) {
            handler.accept(genericCallback);
        }
        for (final Map.Entry<Token, Callback> tokenCallback : tokenCallbacks) {
            if (tokenCallback.getKey().equals(token)) {
                handler.accept(tokenCallback.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return (genericCallback == null ? "" : "generic: " + genericCallback + "; ") +
            (tokenCallbacks.isEmpty() ? "" : "token: " + tokenCallbacks);
    }

}
