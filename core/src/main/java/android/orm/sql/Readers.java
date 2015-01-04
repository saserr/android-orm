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

import static android.orm.util.Maybes.nothing;

public final class Readers {

    private static final Reader<Object> Empty = new Reader.Base<Object>() {

        private final Producer<Maybe<Object>> mNothing = Producers.constant(nothing());

        @NonNull
        @Override
        public Select.Projection getProjection() {
            return Select.Projection.Nothing;
        }

        @NonNull
        @Override
        public Producer<Maybe<Object>> read(@NonNull final Readable input) {
            return mNothing;
        }
    };

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Reader<V> empty() {
        return (Reader<V>) Empty;
    }

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

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Reader<V> safeCast(@NonNull final Reader<? extends V> value) {
        return (Reader<V>) value;
    }

    private static class Composition<V, T> extends Reader.Base<Pair<V, T>> {

        @NonNull
        private final Reader<V> mFirst;
        @NonNull
        private final Reader<T> mSecond;
        @NonNull
        private final Select.Projection mProjection;

        private Composition(@NonNull final Reader<V> first, @NonNull final Reader<T> second) {
            super();

            mFirst = first;
            mSecond = second;
            mProjection = mFirst.getProjection().and(mSecond.getProjection());
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mProjection;
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
        public final Select.Projection getProjection() {
            return mReader.getProjection();
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
