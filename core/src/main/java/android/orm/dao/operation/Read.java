/*
 * Copyright 2013 the original author or authors
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

package android.orm.dao.operation;

import android.orm.sql.Readable;
import android.orm.sql.Reader;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.util.Pair;

import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Read<V> extends Function.Base<Maybe<Pair<Reader<V>, Readable>>, Maybe<Producer<Maybe<V>>>> {

    public Read() {
        super();
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<V>>> invoke(@NonNull final Maybe<Pair<Reader<V>, Readable>> value) {
        final Maybe<Producer<Maybe<V>>> result;

        if (value.isSomething()) {
            final Pair<Reader<V>, Readable> pair = value.get();
            if (pair == null) {
                result = nothing();
            } else {
                final Readable input = pair.second;
                try {
                    result = something(pair.first.read(input));
                } finally {
                    input.close();
                }
            }
        } else {
            result = nothing();
        }

        return result;
    }
}
