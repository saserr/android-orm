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

public final class Functions {

    @NonNull
    public static <V, T> Function<V, T> constant(@NonNull final T value) {
        return new Constant<>(value);
    }

    @NonNull
    public static <V, T, U> Function<V, U> compose(@NonNull final Function<V, T> first,
                                                   @NonNull final Function<? super T, ? extends U> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    public static <V, T, U> Function<V, Pair<T, U>> combine(@NonNull final Function<V, T> first,
                                                            @NonNull final Function<? super V, ? extends U> second) {
        return new Combination<>(first, second);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V, T> Function<V, T> safeCast(@NonNull final Function<? super V, ? extends T> function) {
        return (Function<V, T>) function;
    }

    private static class Constant<V, T> implements Function<V, T> {

        @NonNull
        private final T mValue;

        private Constant(@NonNull final T value) {
            super();

            mValue = value;
        }

        @NonNull
        @Override
        public final T invoke(@NonNull final V ignored) {
            return mValue;
        }
    }

    private static class Composition<V, T, U> implements Function<V, U> {

        @NonNull
        private final Function<V, T> mFirst;
        @NonNull
        private final Function<? super T, ? extends U> mSecond;

        private Composition(@NonNull final Function<V, T> first,
                            @NonNull final Function<? super T, ? extends U> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final U invoke(@NonNull final V value) {
            return mSecond.invoke(mFirst.invoke(value));
        }
    }

    private static class Combination<V, T, U> implements Function<V, Pair<T, U>> {

        @NonNull
        private final Function<V, T> mFirst;
        @NonNull
        private final Function<? super V, ? extends U> mSecond;

        private Combination(@NonNull final Function<V, T> first,
                            @NonNull final Function<? super V, ? extends U> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final Pair<T, U> invoke(@NonNull final V value) {
            return Pair.<T, U>create(mFirst.invoke(value), mSecond.invoke(value));
        }
    }

    private Functions() {
        super();
    }
}
