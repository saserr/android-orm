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

public interface Reader<V> {

    @NonNull
    Select.Projection getProjection();

    @NonNull
    Producer<Maybe<V>> read(@NonNull final Readable input);

    @NonNull
    <T> Reader<T> map(@NonNull final Function<? super V, ? extends T> converter);

    @NonNull
    <T> Reader<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter);

    @NonNull
    <T> Reader<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

    interface Collection<V> extends Reader<V> {

        @NonNull
        <T> Collection<Pair<V, T>> and(@NonNull final Collection<T> other);

        @NonNull
        @Override
        <T> Collection<T> map(@NonNull final Function<? super V, ? extends T> converter);

        @NonNull
        @Override
        <T> Collection<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter);

        @NonNull
        @Override
        <T> Collection<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

        abstract class Base<V> implements Collection<V> {

            @NonNull
            @Override
            public final <T> Collection<Pair<V, T>> and(@NonNull final Collection<T> other) {
                return Readers.compose(this, other);
            }

            @NonNull
            @Override
            public final <T> Collection<T> map(@NonNull final Function<? super V, ? extends T> converter) {
                return convert(Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Collection<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter) {
                return convert(Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Collection<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return Readers.convert(this, converter);
            }
        }
    }

    interface Element<V> extends Reader<V> {

        @NonNull
        <T> Element<Pair<V, T>> and(@NonNull final Element<T> other);

        @NonNull
        @Override
        <T> Element<T> map(@NonNull final Function<? super V, ? extends T> converter);

        @NonNull
        @Override
        <T> Element<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter);

        @NonNull
        @Override
        <T> Element<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

        abstract class Create<V> implements Element<V> {

            protected Create() {
                super();
            }

            @NonNull
            @Override
            public final <T> Element<Pair<V, T>> and(@NonNull final Element<T> other) {
                return (other instanceof Create) ?
                        new Composition<>(this, (Create<T>) other) :
                        new Update.Composition<>(this, other);
            }

            @NonNull
            public final <T> Create<Pair<V, T>> and(@NonNull final Create<T> other) {
                return new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <T> Create<T> map(@NonNull final Function<? super V, ? extends T> converter) {
                return convert(Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Create<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter) {
                return convert(Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Create<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return new Conversion<>(this, converter);
            }

            private static class Composition<V, T> extends Create<Pair<V, T>> {

                @NonNull
                private final Create<V> mFirst;
                @NonNull
                private final Create<T> mSecond;
                @NonNull
                private final Select.Projection mProjection;
                @NonNull
                private final Function<Pair<Maybe<V>, Maybe<T>>, Maybe<Pair<V, T>>> mLift;

                private Composition(@NonNull final Create<V> first,
                                    @NonNull final Create<T> second) {
                    super();

                    mFirst = first;
                    mSecond = second;
                    mProjection = mFirst.getProjection().and(mSecond.getProjection());
                    mLift = Maybes.liftPair();
                }

                @NonNull
                @Override
                public final Select.Projection getProjection() {
                    return mProjection;
                }

                @NonNull
                @Override
                public final Producer<Maybe<Pair<V, T>>> read(@NonNull final Readable input) {
                    final Maybe<V> result1 = mFirst.read(input).produce();
                    final Maybe<T> result2 = mSecond.read(input).produce();
                    return Producers.constant(mLift.invoke(Pair.create(result1, result2)));
                }
            }

            private static class Conversion<V, T> extends Create<T> {

                @NonNull
                private final Create<V> mCreate;
                @NonNull
                private final Function<Maybe<V>, Maybe<T>> mConverter;

                private Conversion(@NonNull final Create<V> create,
                                   @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                    super();

                    mCreate = create;
                    mConverter = converter;
                }

                @NonNull
                @Override
                public final Select.Projection getProjection() {
                    return mCreate.getProjection();
                }

                @NonNull
                @Override
                public final Producer<Maybe<T>> read(@NonNull final Readable input) {
                    final Maybe<V> result = mCreate.read(input).produce();
                    return Producers.constant(mConverter.invoke(result));
                }
            }
        }

        abstract class Update<V> implements Element<V> {

            protected Update() {
                super();
            }

            @NonNull
            @Override
            public final <T> Update<Pair<V, T>> and(@NonNull final Element<T> other) {
                return new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <T> Update<T> map(@NonNull final Function<? super V, ? extends T> converter) {
                return convert(Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Update<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter) {
                return convert(Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Update<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return new Conversion<>(this, converter);
            }

            private static class Composition<V, T> extends Update<Pair<V, T>> {

                @NonNull
                private final Element<V> mFirst;
                @NonNull
                private final Element<T> mSecond;
                @NonNull
                private final Select.Projection mProjection;

                private Composition(@NonNull final Element<V> first,
                                    @NonNull final Element<T> second) {
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
                    final Producer<Maybe<V>> result1 = mFirst.read(input);
                    final Producer<Maybe<T>> result2 = mSecond.read(input);
                    return Producers.convert(Producers.compose(result1, result2), Maybes.<V, T>liftPair());
                }
            }

            private static class Conversion<V, T> extends Update<T> {

                @NonNull
                private final Update<V> mUpdate;
                @NonNull
                private final Function<Maybe<V>, Maybe<T>> mConverter;

                private Conversion(@NonNull final Update<V> update,
                                   @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                    super();

                    mUpdate = update;
                    mConverter = converter;
                }

                @NonNull
                @Override
                public final Select.Projection getProjection() {
                    return mUpdate.getProjection();
                }

                @NonNull
                @Override
                public final Producer<Maybe<T>> read(@NonNull final Readable input) {
                    return Producers.convert(mUpdate.read(input), mConverter);
                }
            }
        }
    }
}
