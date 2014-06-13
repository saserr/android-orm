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

public final class Converters {

    @NonNull
    public static <V, T> Function<V, T> from(@NonNull final Converter<? super V, ? extends T> converter) {
        return new From<>(converter);
    }

    @NonNull
    public static <V, T> Function<T, V> to(@NonNull final Converter<? extends V, ? super T> converter) {
        return new To<>(converter);
    }

    private static class From<V, T> implements Function<V, T> {

        @NonNull
        private final Converter<? super V, ? extends T> mConverter;

        private From(@NonNull final Converter<? super V, ? extends T> converter) {
            super();

            mConverter = converter;
        }

        @NonNull
        @Override
        public final T invoke(@NonNull final V v) {
            return mConverter.from(v);
        }
    }

    private static class To<V, T> implements Function<V, T> {

        @NonNull
        private final Converter<? extends T, ? super V> mConverter;

        private To(@NonNull final Converter<? extends T, ? super V> converter) {
            super();

            mConverter = converter;
        }

        @NonNull
        @Override
        public final T invoke(@NonNull final V v) {
            return mConverter.to(v);
        }
    }

    private Converters() {
        super();
    }
}
