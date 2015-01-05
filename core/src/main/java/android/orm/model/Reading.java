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

package android.orm.model;

import android.orm.sql.Reader;
import android.orm.util.Converter;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Pair;

public interface Reading<M> {

    @NonNull
    Reader.Collection<M> prepareReader();

    @NonNull
    <N> Reading<N> map(@NonNull final Converter<M, N> converter);

    @NonNull
    <N> Reading<N> convert(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

    interface Single<M> extends Reading<M> {

        @NonNull
        Reader.Collection<M> prepareReader(@NonNull final M m);

        @NonNull
        <N> Single<Pair<M, N>> and(@NonNull final Single<N> other);

        @NonNull
        @Override
        <N> Single<N> map(@NonNull final Converter<M, N> converter);

        @NonNull
        @Override
        <N> Single<N> convert(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

        abstract class Base<M> implements Single<M> {

            @NonNull
            @Override
            public final <N> Single<Pair<M, N>> and(@NonNull final Single<N> other) {
                return Readings.compose(this, other);
            }

            @NonNull
            @Override
            public final <N> Single<N> map(@NonNull final Converter<M, N> converter) {
                return convert(Maybes.lift(converter));
            }

            @NonNull
            @Override
            public final <N> Single<N> convert(@NonNull final Converter<Maybe<M>, Maybe<N>> converter) {
                return Readings.convert(this, converter);
            }
        }
    }

    interface Many<M> extends Reading<M> {

        @NonNull
        <N> Many<Pair<M, N>> and(@NonNull final Many<N> other);

        @NonNull
        @Override
        <N> Many<N> map(@NonNull final Converter<M, N> converter);

        @NonNull
        @Override
        <N> Many<N> convert(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

        abstract class Base<M> implements Many<M> {

            @NonNull
            @Override
            public final <N> Many<Pair<M, N>> and(@NonNull final Many<N> other) {
                return Readings.compose(this, other);
            }

            @NonNull
            @Override
            public final <N> Many<N> map(@NonNull final Converter<M, N> converter) {
                return convert(Maybes.lift(converter));
            }

            @NonNull
            @Override
            public final <N> Many<N> convert(@NonNull final Converter<Maybe<M>, Maybe<N>> converter) {
                return Readings.convert(this, converter);
            }
        }
    }
}
