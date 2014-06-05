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
import android.support.annotation.NonNull;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

public interface AggregateFunction<V> extends Value.Read<V> {

    @NonNull
    <T> AggregateFunction<Pair<V, T>> and(@NonNull final AggregateFunction<T> other);

    @NonNull
    @Override
    <T> AggregateFunction<T> mapTo(@NonNull final Function<? super V, ? extends T> converter);

    @NonNull
    @Override
    <T> AggregateFunction<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter);

    @NonNull
    @Override
    <T> AggregateFunction<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

    abstract class Base<V> implements AggregateFunction<V> {

        @NonNull
        @Override
        public final <T> Value.Read<Pair<V, T>> and(@NonNull final Value.Read<T> other) {
            return Values.compose(this, other);
        }

        @NonNull
        @Override
        public final <T> AggregateFunction<Pair<V, T>> and(@NonNull final AggregateFunction<T> other) {
            return AggregateFunctions.compose(this, other);
        }

        @NonNull
        @Override
        public final <T> AggregateFunction<T> mapTo(@NonNull final Function<? super V, ? extends T> converter) {
            return AggregateFunctions.convert(this, Maybes.map(converter));
        }

        @NonNull
        @Override
        public final <T> AggregateFunction<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter) {
            return AggregateFunctions.convert(this, Maybes.flatMap(converter));
        }

        @NonNull
        @Override
        public final <T> AggregateFunction<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            return AggregateFunctions.convert(this, converter);
        }
    }

    interface Builder<V> {
        @NonNull
        AggregateFunction<V> as(@NonNls @NonNull final String name);
    }
}
