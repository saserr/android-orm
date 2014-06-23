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
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.util.Pair;

public final class Readers {

    @NonNull
    public static <V, T> Reader<Pair<V, T>> compose(@NonNull final Reader<V> first,
                                                    @NonNull final Reader<T> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    public static <V, T> Reader<T> convert(@NonNull final Reader<V> reader,
                                           @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new Conversion<>(reader, converter);
    }

    private static class Composition<V, T> extends Reader.Base<Pair<V, T>> {

        @NonNull
        private final Reader<V> mFirst;
        @NonNull
        private final Reader<T> mSecond;

        private Composition(@NonNull final Reader<V> first, @NonNull final Reader<T> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final Producer<Maybe<Pair<V, T>>> read(@NonNull final Readable input) {
            return Producers.convert(
                    Producers.compose(mFirst.read(input), mSecond.read(input)),
                    Maybes.<V, T>liftPair()
            );
        }
    }

    private static class Conversion<V, T> extends Reader.Base<T> {

        @NonNull
        private final Reader<V> mReader;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mConverter;

        private Conversion(@NonNull final Reader<V> reader,
                           @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            super();

            mReader = reader;
            mConverter = converter;
        }

        @NonNull
        @Override
        public final Producer<Maybe<T>> read(@NonNull final Readable input) {
            return Producers.convert(mReader.read(input), mConverter);
        }
    }

    private Readers() {
        super();
    }
}
