/*
 * Copyright 2014 the original author or authors
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

package android.orm.sql;

import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.util.Pair;

public interface Reader<V> {

    @NonNull
    Select.Projection getProjection();

    @NonNull
    Producer<Maybe<V>> read(@NonNull final Readable input);

    @NonNull
    <T> Reader<Pair<V, T>> and(@NonNull final Reader<T> other);

    @NonNull
    <T> Reader<T> map(@NonNull final Function<? super V, ? extends T> converter);

    @NonNull
    <T> Reader<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter);

    @NonNull
    <T> Reader<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

    abstract class Base<V> implements Reader<V> {

        @NonNull
        @Override
        public final <T> Reader<Pair<V, T>> and(@NonNull final Reader<T> other) {
            return Readers.compose(this, other);
        }

        @NonNull
        @Override
        public final <T> Reader<T> map(@NonNull final Function<? super V, ? extends T> converter) {
            return convert(Maybes.map(converter));
        }

        @NonNull
        @Override
        public final <T> Reader<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter) {
            return convert(Maybes.flatMap(converter));
        }

        @NonNull
        @Override
        public final <T> Reader<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            return Readers.convert(this, converter);
        }
    }
}
