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

public final class Functions {

    @NonNull
    public static <V, T, U> Function<V, U> compose(@NonNull final Function<V, T> first,
                                                   @NonNull final Function<? super T, ? extends U> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V, T> Function<V, T> safeCast(@NonNull final Function<? super V, ? extends T> function) {
        return (Function<V, T>) function;
    }

    private static class Composition<V, T, U> extends Function.Base<V, U> {

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

    private Functions() {
        super();
    }
}
