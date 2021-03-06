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

package android.orm.util;

import android.support.annotation.NonNull;
import android.util.Pair;

public final class Producers {

    @NonNull
    public static <V> Producer<V> constant(@NonNull final V value) {
        return new Constant<>(value);
    }

    @NonNull
    public static <V, T> Producer<T> convert(@NonNull final Producer<V> producer,
                                             @NonNull final Function<? super V, ? extends T> converter) {
        return new Conversion<>(producer, converter);
    }

    @NonNull
    public static <V, T> Producer<Pair<V, T>> compose(@NonNull final Producer<? extends V> first,
                                                      @NonNull final Producer<? extends T> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Producer<V> safeCast(@NonNull final Producer<? extends V> producer) {
        return (Producer<V>) producer;
    }

    private static class Constant<V> implements Producer<V> {

        @NonNull
        private final V mValue;

        private Constant(@NonNull final V value) {
            super();

            mValue = value;
        }

        @NonNull
        @Override
        public final V produce() {
            return mValue;
        }
    }

    private static class Conversion<V, T> implements Producer<T> {

        @NonNull
        private final Producer<V> mProducer;
        @NonNull
        private final Function<? super V, ? extends T> mFunction;

        private Conversion(@NonNull final Producer<V> producer,
                           @NonNull final Function<? super V, ? extends T> function) {
            super();

            mProducer = producer;
            mFunction = function;
        }

        @NonNull
        @Override
        public final T produce() {
            return mFunction.invoke(mProducer.produce());
        }
    }

    private static class Composition<V, T> implements Producer<Pair<V, T>> {

        @NonNull
        private final Producer<? extends V> mFirst;
        @NonNull
        private final Producer<? extends T> mSecond;

        private Composition(@NonNull final Producer<? extends V> first,
                            @NonNull final Producer<? extends T> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final Pair<V, T> produce() {
            return Pair.<V, T>create(mFirst.produce(), mSecond.produce());
        }
    }

    private Producers() {
        super();
    }
}
